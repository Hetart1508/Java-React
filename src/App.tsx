import { useCallback, useEffect, useState } from "react";
import axios from "axios";
import { IndianRupee, LogOut } from "lucide-react";
import { AuthUser, PaginatedTransactions, SummaryStats, Transaction, TransactionQuery } from "./types";
import AuthPanel from "./components/AuthPanel";
import SummaryDashboard from "./components/SummaryDashboard";
import TransactionForm from "./components/TransactionForm";
import TransactionList from "./components/TransactionList";

axios.defaults.baseURL = import.meta.env.VITE_API_BASE_URL || "";
axios.defaults.withCredentials = true;

const initialQuery: TransactionQuery = {
  type: "",
  category: "",
  date: "",
  page: 1,
  pageSize: 10,
  sortBy: "date",
  sortDir: "desc",
};

export default function App() {
  const [currentUser, setCurrentUser] = useState<AuthUser | null>(null);
  const [checkingSession, setCheckingSession] = useState(true);
  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [stats, setStats] = useState<SummaryStats>({
    totalIncome: 0,
    totalExpense: 0,
    currentBalance: 0,
  });
  const [query, setQuery] = useState<TransactionQuery>(initialQuery);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [editingTransaction, setEditingTransaction] = useState<Transaction | null>(null);

  const checkSession = useCallback(async () => {
    try {
      const response = await axios.get<AuthUser>("/api/auth/me");
      setCurrentUser(response.data);
    } catch {
      setCurrentUser(null);
    } finally {
      setCheckingSession(false);
    }
  }, []);

  const fetchSummary = useCallback(async () => {
    const response = await axios.get<SummaryStats>("/api/transactions/summary");
    setStats(response.data);
  }, []);

  const fetchTransactions = useCallback(async () => {
    setLoading(true);
    setError(null);

    try {
      const params = Object.fromEntries(
        Object.entries(query).filter(([, value]) => value !== "")
      );
      const response = await axios.get<PaginatedTransactions | Transaction[]>("/api/transactions", { params });

      if (Array.isArray(response.data)) {
        setTransactions(response.data);
        setTotal(response.data.length);
      } else {
        setTransactions(response.data.items);
        setTotal(response.data.total);
      }
    } catch (err: any) {
      setError(err.response?.data?.error || err.message || "Unable to load transactions.");
    } finally {
      setLoading(false);
    }
  }, [query]);

  useEffect(() => {
    checkSession();
  }, [checkSession]);

  useEffect(() => {
    if (currentUser) {
      fetchTransactions();
      fetchSummary().catch(() => undefined);
    }
  }, [currentUser, fetchTransactions, fetchSummary]);

  const handleSaveTransaction = async (
    transactionData: Omit<Transaction, "id"> & { id?: number },
    method: "POST" | "PUT"
  ) => {
    if (method === "POST") {
      await axios.post("/api/transactions", transactionData);
    } else {
      await axios.put(`/api/transactions/${transactionData.id}`, transactionData);
      setEditingTransaction(null);
    }

    await fetchTransactions();
    await fetchSummary();
  };

  const handleDeleteTransaction = async (id: number) => {
    await axios.delete(`/api/transactions/${id}`);
    await fetchTransactions();
    await fetchSummary();
  };

  const handleEditInit = (transaction: Transaction) => {
    setEditingTransaction(transaction);
    document.getElementById("transaction-form-container")?.scrollIntoView({ behavior: "smooth" });
  };

  const handleAuthenticated = (user: AuthUser) => {
    setCurrentUser(user);
    setQuery(initialQuery);
    setEditingTransaction(null);
  };

  const handleLogout = async () => {
    await axios.post("/api/auth/logout");
    setCurrentUser(null);
    setTransactions([]);
    setStats({
      totalIncome: 0,
      totalExpense: 0,
      currentBalance: 0,
    });
    setTotal(0);
    setEditingTransaction(null);
    setQuery(initialQuery);
  };

  if (checkingSession) {
    return (
      <div className="min-h-screen bg-slate-50 text-slate-500 flex items-center justify-center">
        <div className="text-sm font-medium">Checking session...</div>
      </div>
    );
  }

  if (!currentUser) {
    return <AuthPanel onAuthenticated={handleAuthenticated} />;
  }

  return (
    <div className="min-h-screen bg-slate-50 text-slate-900 font-sans">
      <nav className="bg-white border-b border-slate-200 sticky top-0 z-30">
        <div className="max-w-7xl mx-auto px-6 h-16 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="w-9 h-9 bg-indigo-600 rounded-lg flex items-center justify-center text-white">
              <IndianRupee size={18} />
            </div>
            <div>
              <h1 className="text-lg font-semibold tracking-tight text-slate-950">Finance Manager</h1>
              <p className="text-xs text-slate-500">Transactions</p>
            </div>
          </div>

          <div className="flex items-center gap-3">
            <div className="text-right hidden sm:block">
              <div className="text-xs font-semibold text-slate-900">{currentUser.name}</div>
              <div className="text-[11px] text-slate-500">{currentUser.email}</div>
            </div>
            <button
              type="button"
              onClick={handleLogout}
              className="p-2 border border-slate-200 rounded-lg text-slate-500 hover:text-slate-900 hover:bg-slate-50"
              title="Logout"
            >
              <LogOut size={16} />
            </button>
          </div>
        </div>
      </nav>

      <main className="max-w-7xl w-full mx-auto p-6 md:p-8 space-y-6">
        <SummaryDashboard stats={stats} loading={loading} />

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
          <div className="lg:col-span-1">
            <TransactionForm
              onSave={handleSaveTransaction}
              editingTransaction={editingTransaction}
              onCancelEdit={() => setEditingTransaction(null)}
            />
          </div>

          <div className="lg:col-span-2">
            <TransactionList
              transactions={transactions}
              total={total}
              loading={loading}
              error={error}
              query={query}
              onQueryChange={setQuery}
              onEdit={handleEditInit}
              onDelete={handleDeleteTransaction}
            />
          </div>
        </div>
      </main>
    </div>
  );
}

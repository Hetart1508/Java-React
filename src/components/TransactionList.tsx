import { Transaction, TransactionQuery, SortField } from "../types";
import {
  ArrowDown,
  ArrowDownRight,
  ArrowUp,
  ArrowUpRight,
  Calendar,
  ChevronLeft,
  ChevronRight,
  Edit2,
  Filter,
  Search,
  Trash2,
} from "lucide-react";

interface TransactionListProps {
  transactions: Transaction[];
  total: number;
  loading: boolean;
  error: string | null;
  query: TransactionQuery;
  onQueryChange: (query: TransactionQuery) => void;
  onEdit: (transaction: Transaction) => void;
  onDelete: (id: number) => Promise<void>;
}

const CATEGORIES = [
  "Salary",
  "Freelance",
  "Food",
  "Rent",
  "Utilities",
  "Entertainment",
  "Shopping",
  "Transportation",
  "Healthcare",
  "Education",
  "Investments",
  "Other",
];

const SORTABLE_COLUMNS: { key: SortField; label: string; align?: string }[] = [
  { key: "date", label: "Date" },
  { key: "title", label: "Title" },
  { key: "category", label: "Category" },
  { key: "type", label: "Type" },
  { key: "amount", label: "Amount", align: "text-right" },
];

export default function TransactionList({
  transactions,
  total,
  loading,
  error,
  query,
  onQueryChange,
  onEdit,
  onDelete,
}: TransactionListProps) {
  const totalPages = Math.max(1, Math.ceil(total / query.pageSize));
  const startItem = total === 0 ? 0 : (query.page - 1) * query.pageSize + 1;
  const endItem = Math.min(query.page * query.pageSize, total);
  const hasActiveFilters = query.type || query.category || query.date;

  const updateQuery = (updates: Partial<TransactionQuery>) => {
    onQueryChange({ ...query, ...updates });
  };

  const setFilter = (updates: Partial<TransactionQuery>) => {
    updateQuery({ ...updates, page: 1 });
  };

  const clearFilters = () => {
    onQueryChange({
      ...query,
      type: "",
      category: "",
      date: "",
      page: 1,
    });
  };

  const handleSort = (sortBy: SortField) => {
    const sortDir = query.sortBy === sortBy && query.sortDir === "asc" ? "desc" : "asc";
    updateQuery({ sortBy, sortDir, page: 1 });
  };

  const formatCurrency = (val: number) => {
    return new Intl.NumberFormat("en-IN", {
      style: "currency",
      currency: "INR",
    }).format(val);
  };

  return (
    <div className="bg-white border border-slate-200 rounded-lg shadow-sm overflow-hidden">
      <div className="p-6 border-b border-slate-100 bg-slate-50/50">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 mb-6">
          <div>
            <h2 className="text-sm font-semibold text-slate-900 tracking-tight">Transactions</h2>
            <p className="text-xs text-slate-500 mt-0.5">Filter, sort, edit, and delete records</p>
          </div>

          <div className="flex items-center gap-3">
            <span className="text-xs text-slate-500">
              {startItem}-{endItem} of {total}
            </span>
            {hasActiveFilters && (
              <button
                onClick={clearFilters}
                className="text-xs font-semibold text-indigo-600 hover:text-indigo-800"
              >
                Reset filters
              </button>
            )}
          </div>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <div>
            <label className="text-[11px] font-semibold text-slate-500 uppercase mb-1.5 flex items-center gap-1">
              <Filter size={12} /> Type
            </label>
            <div className="flex bg-slate-100 p-0.5 rounded-lg border border-slate-200">
              {["", "INCOME", "EXPENSE"].map((type) => (
                <button
                  key={type || "ALL"}
                  onClick={() => setFilter({ type })}
                  className={`flex-1 text-center text-xs py-1.5 rounded-md font-semibold transition-colors ${
                    query.type === type
                      ? "bg-white text-indigo-600 shadow-sm"
                      : "text-slate-600 hover:text-slate-900"
                  }`}
                >
                  {type ? type.charAt(0) + type.slice(1).toLowerCase() : "All"}
                </button>
              ))}
            </div>
          </div>

          <div>
            <label className="text-[11px] font-semibold text-slate-500 uppercase mb-1.5 flex items-center gap-1">
              <Search size={12} /> Category
            </label>
            <select
              value={query.category}
              onChange={(e) => setFilter({ category: e.target.value })}
              className="w-full bg-white border border-slate-200 rounded-lg px-3 py-2 text-xs text-slate-700 outline-none focus:ring-2 focus:ring-indigo-100 focus:border-indigo-600"
            >
              <option value="">All Categories</option>
              {CATEGORIES.map((cat) => (
                <option key={cat} value={cat}>
                  {cat}
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="text-[11px] font-semibold text-slate-500 uppercase mb-1.5 flex items-center gap-1">
              <Calendar size={12} /> Date
            </label>
            <input
              type="date"
              value={query.date}
              onChange={(e) => setFilter({ date: e.target.value })}
              className="w-full bg-white border border-slate-200 rounded-lg px-3 py-2 text-xs text-slate-700 outline-none focus:ring-2 focus:ring-indigo-100 focus:border-indigo-600"
            />
          </div>
        </div>
      </div>

      <div className="overflow-x-auto">
        {loading && transactions.length === 0 ? (
          <div className="p-12 text-center text-slate-500">
            <div className="animate-spin inline-block w-8 h-8 border-4 border-indigo-500 border-t-transparent rounded-full mb-4"></div>
            <p className="text-sm font-medium">Loading transactions...</p>
          </div>
        ) : error ? (
          <div className="p-12 text-center text-rose-600">
            <p className="text-sm font-semibold mb-2">Unable to load transactions</p>
            <p className="text-xs bg-rose-50 border border-rose-100 rounded-lg p-3 inline-block max-w-md">
              {error}
            </p>
          </div>
        ) : transactions.length === 0 ? (
          <div className="p-12 text-center text-slate-500">
            <p className="text-sm font-medium mb-1">No transactions found</p>
            <p className="text-xs text-slate-400 mb-4">Try changing the filters or add a new transaction.</p>
            {hasActiveFilters && (
              <button
                onClick={clearFilters}
                className="bg-indigo-50 text-indigo-700 text-xs font-semibold py-1.5 px-3 rounded-lg hover:bg-indigo-100"
              >
                Clear filters
              </button>
            )}
          </div>
        ) : (
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="border-b border-slate-100 bg-slate-50/50 text-[11px] text-slate-500 uppercase">
                {SORTABLE_COLUMNS.map((column) => (
                  <th key={column.key} className={`py-3 px-6 font-semibold ${column.align || ""}`}>
                    <button
                      onClick={() => handleSort(column.key)}
                      className={`inline-flex items-center gap-1 hover:text-slate-900 ${column.align || ""}`}
                    >
                      {column.label}
                      {query.sortBy === column.key &&
                        (query.sortDir === "asc" ? <ArrowUp size={12} /> : <ArrowDown size={12} />)}
                    </button>
                  </th>
                ))}
                <th className="py-3 px-6 font-semibold text-center">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 text-sm">
              {transactions.map((t) => (
                <tr key={t.id} className="hover:bg-slate-50 transition-colors">
                  <td className="py-3.5 px-6 text-xs text-slate-500">{t.date}</td>
                  <td className="py-3.5 px-6">
                    <div className="font-medium text-slate-900">{t.title}</div>
                    {t.description && (
                      <div className="text-xs text-slate-500 max-w-xs truncate mt-0.5" title={t.description}>
                        {t.description}
                      </div>
                    )}
                  </td>
                  <td className="py-3.5 px-6">
                    <span className="inline-block text-xs font-medium text-slate-600 bg-slate-100 px-2.5 py-1 rounded-full">
                      {t.category}
                    </span>
                  </td>
                  <td className="py-3.5 px-6">
                    <span
                      className={`inline-flex items-center gap-1 text-[11px] font-bold px-2 py-0.5 rounded ${
                        t.type === "INCOME"
                          ? "text-emerald-700 bg-emerald-50"
                          : "text-rose-700 bg-rose-50"
                      }`}
                    >
                      {t.type === "INCOME" ? <ArrowUpRight size={10} /> : <ArrowDownRight size={10} />}
                      {t.type}
                    </span>
                  </td>
                  <td
                    className={`py-3.5 px-6 text-right font-mono font-semibold ${
                      t.type === "INCOME" ? "text-emerald-600" : "text-slate-900"
                    }`}
                  >
                    {t.type === "INCOME" ? "+" : "-"}
                    {formatCurrency(t.amount)}
                  </td>
                  <td className="py-3.5 px-6">
                    <div className="flex items-center justify-center gap-1.5">
                      <button
                        onClick={() => onEdit(t)}
                        className="p-1.5 hover:bg-slate-100 text-slate-500 hover:text-slate-950 rounded-lg"
                        title="Edit"
                      >
                        <Edit2 size={14} />
                      </button>
                      <button
                        onClick={() => {
                          if (window.confirm("Delete this transaction?")) {
                            onDelete(t.id);
                          }
                        }}
                        className="p-1.5 hover:bg-rose-50 text-rose-500 hover:text-rose-700 rounded-lg"
                        title="Delete"
                      >
                        <Trash2 size={14} />
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 px-6 py-4 border-t border-slate-100 bg-white">
        <div className="flex items-center gap-2">
          <span className="text-xs text-slate-500">Rows per page</span>
          <select
            value={query.pageSize}
            onChange={(e) => updateQuery({ pageSize: Number(e.target.value), page: 1 })}
            className="border border-slate-200 rounded-lg px-2 py-1 text-xs"
          >
            {[5, 10, 20, 50].map((size) => (
              <option key={size} value={size}>
                {size}
              </option>
            ))}
          </select>
        </div>

        <div className="flex items-center gap-3">
          <span className="text-xs text-slate-500">
            Page {query.page} of {totalPages}
          </span>
          <div className="flex items-center gap-1">
            <button
              onClick={() => updateQuery({ page: Math.max(1, query.page - 1) })}
              disabled={query.page <= 1}
              className="p-2 border border-slate-200 rounded-lg text-slate-600 disabled:opacity-40 disabled:cursor-not-allowed hover:bg-slate-50"
              title="Previous page"
            >
              <ChevronLeft size={14} />
            </button>
            <button
              onClick={() => updateQuery({ page: Math.min(totalPages, query.page + 1) })}
              disabled={query.page >= totalPages}
              className="p-2 border border-slate-200 rounded-lg text-slate-600 disabled:opacity-40 disabled:cursor-not-allowed hover:bg-slate-50"
              title="Next page"
            >
              <ChevronRight size={14} />
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}

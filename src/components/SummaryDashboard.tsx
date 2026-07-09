import { SummaryStats } from "../types";
import { TrendingUp, TrendingDown, IndianRupee } from "lucide-react";

interface SummaryDashboardProps {
  stats: SummaryStats;
  loading: boolean;
}

export default function SummaryDashboard({ stats, loading }: SummaryDashboardProps) {
  const formatCurrency = (val: number) => {
    return new Intl.NumberFormat("en-IN", {
      style: "currency",
      currency: "INR",
    }).format(val);
  };

  return (
    <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8" id="summary-dashboard">
      
      {/* Total Balance Card */}
      <div 
        className="bg-white p-5 rounded-2xl shadow-sm border border-slate-100 flex items-center justify-between transition-all hover:border-slate-200"
        id="card-balance"
      >
        <div>
          <p className="text-xs font-medium text-slate-500 uppercase tracking-wider mb-1">Total Balance</p>
          <h2 className="text-2xl font-bold text-slate-900 font-mono">
            {loading ? (
              <span className="inline-block w-24 h-6 bg-slate-100 animate-pulse rounded" />
            ) : (
              formatCurrency(stats.currentBalance)
            )}
          </h2>
          <p className="text-[10px] text-slate-400 font-mono mt-1 flex items-center gap-1">
            <IndianRupee size={10} /> Net remaining balance
          </p>
        </div>
        <div className="p-3 bg-slate-50 text-slate-500 rounded-xl border border-slate-100">
          <IndianRupee size={20} />
        </div>
      </div>

      {/* Total Income Card */}
      <div 
        className="bg-white p-5 rounded-2xl shadow-sm border border-slate-100 flex items-center justify-between transition-all hover:border-slate-200"
        id="card-income"
      >
        <div>
          <p className="text-xs font-medium text-emerald-600 uppercase tracking-wider mb-1">Income</p>
          <h2 className="text-2xl font-bold text-emerald-600 font-mono">
            {loading ? (
              <span className="inline-block w-24 h-6 bg-slate-100 animate-pulse rounded" />
            ) : (
              `+${formatCurrency(stats.totalIncome)}`
            )}
          </h2>
          <p className="text-[10px] text-emerald-500 font-mono mt-1 flex items-center gap-1">
            <TrendingUp size={10} /> Positive financial flow
          </p>
        </div>
        <div className="p-3 bg-emerald-50/50 text-emerald-600 rounded-xl border border-emerald-100/50">
          <TrendingUp size={20} />
        </div>
      </div>

      {/* Total Expenses Card */}
      <div 
        className="bg-white p-5 rounded-2xl shadow-sm border border-slate-100 flex items-center justify-between transition-all hover:border-slate-200"
        id="card-expenses"
      >
        <div>
          <p className="text-xs font-medium text-rose-500 uppercase tracking-wider mb-1">Expenses</p>
          <h2 className="text-2xl font-bold text-rose-500 font-mono">
            {loading ? (
              <span className="inline-block w-24 h-6 bg-slate-100 animate-pulse rounded" />
            ) : (
              `-${formatCurrency(stats.totalExpense)}`
            )}
          </h2>
          <p className="text-[10px] text-rose-400 font-mono mt-1 flex items-center gap-1">
            <TrendingDown size={10} /> Negative financial flow
          </p>
        </div>
        <div className="p-3 bg-rose-50/50 text-rose-600 rounded-xl border border-rose-100/50">
          <TrendingDown size={20} />
        </div>
      </div>

    </div>
  );
}

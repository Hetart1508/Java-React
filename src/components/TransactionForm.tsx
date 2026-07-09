import React, { useState, useEffect } from "react";
import { Transaction } from "../types";
import { PlusCircle, Edit3, ArrowUpRight, ArrowDownRight, RefreshCcw } from "lucide-react";

interface TransactionFormProps {
  onSave: (transactionData: Omit<Transaction, "id"> & { id?: number }, method: "POST" | "PUT") => Promise<void>;
  editingTransaction: Transaction | null;
  onCancelEdit: () => void;
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

export default function TransactionForm({ onSave, editingTransaction, onCancelEdit }: TransactionFormProps) {
  const [title, setTitle] = useState("");
  const [amount, setAmount] = useState<number | "">("");
  const [type, setType] = useState<"INCOME" | "EXPENSE">("EXPENSE");
  const [category, setCategory] = useState("Food");
  const [date, setDate] = useState(() => {
    const today = new Date();
    return today.toISOString().split("T")[0];
  });
  const [description, setDescription] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  // Synchronize when editing changes
  useEffect(() => {
    if (editingTransaction) {
      setTitle(editingTransaction.title);
      setAmount(editingTransaction.amount);
      setType(editingTransaction.type);
      setCategory(editingTransaction.category);
      setDate(editingTransaction.date);
      setDescription(editingTransaction.description || "");
      setErrorMsg(null);
    } else {
      resetForm();
    }
  }, [editingTransaction]);

  const resetForm = () => {
    setTitle("");
    setAmount("");
    setType("EXPENSE");
    setCategory("Food");
    setDate(new Date().toISOString().split("T")[0]);
    setDescription("");
    setErrorMsg(null);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMsg(null);

    // Form Client-side Validations
    if (!title.trim()) {
      setErrorMsg("Title is required.");
      return;
    }
    if (amount === "" || Number(amount) <= 0) {
      setErrorMsg("Amount must be a positive number greater than zero.");
      return;
    }
    if (!category) {
      setErrorMsg("Please select a category.");
      return;
    }
    if (!date) {
      setErrorMsg("Date is required.");
      return;
    }

    setIsSubmitting(true);
    try {
      const transactionData = {
        title: title.trim(),
        amount: Number(amount),
        type,
        category,
        date,
        description: description.trim(),
        ...(editingTransaction ? { id: editingTransaction.id } : {}),
      };

      const method = editingTransaction ? "PUT" : "POST";
      await onSave(transactionData, method);
      resetForm();
    } catch (err: any) {
      setErrorMsg(err.response?.data?.error || err.message || "Failed to process transaction.");
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="bg-white border border-slate-100 rounded-2xl p-6 shadow-sm" id="transaction-form-container">
      <div className="flex items-center justify-between border-b border-slate-100 pb-4 mb-6">
        <h2 className="text-sm font-semibold text-slate-900 tracking-tight flex items-center gap-2">
          {editingTransaction ? (
            <>
              <Edit3 className="text-amber-600" size={18} />
              <span>Edit Transaction</span>
            </>
          ) : (
            <>
              <PlusCircle className="text-indigo-600" size={18} />
              <span>Add Transaction</span>
            </>
          )}
        </h2>
        
        {editingTransaction && (
          <button
            type="button"
            onClick={onCancelEdit}
            className="text-xs font-semibold text-slate-500 hover:text-slate-900"
          >
            Cancel
          </button>
        )}
      </div>

      <form onSubmit={handleSubmit} className="space-y-4" id="transaction-form">
        {/* Type Toggle */}
        <div>
          <label className="text-[11px] font-semibold text-slate-400 uppercase block mb-1">Type</label>
          <div className="grid grid-cols-2 gap-3">
            <button
              type="button"
              onClick={() => setType("INCOME")}
              className={`flex items-center justify-center gap-2 py-2 px-3 rounded-lg border text-xs font-semibold transition-all ${
                type === "INCOME"
                  ? "bg-emerald-50 border-emerald-500 text-emerald-700 shadow-xs"
                  : "bg-white border-slate-200 text-slate-600 hover:bg-slate-50"
              }`}
              id="form-type-income"
            >
              <ArrowUpRight size={14} />
              Income
            </button>
            <button
              type="button"
              onClick={() => setType("EXPENSE")}
              className={`flex items-center justify-center gap-2 py-2 px-3 rounded-lg border text-xs font-semibold transition-all ${
                type === "EXPENSE"
                  ? "bg-rose-50 border-rose-500 text-rose-700 shadow-xs"
                  : "bg-white border-slate-200 text-slate-600 hover:bg-slate-50"
              }`}
              id="form-type-expense"
            >
              <ArrowDownRight size={14} />
              Expense
            </button>
          </div>
        </div>

        {/* Title Input */}
        <div>
          <label htmlFor="title" className="text-[11px] font-semibold text-slate-400 uppercase block mb-1">
            Title <span className="text-rose-500">*</span>
          </label>
          <input
            id="title"
            type="text"
            required
            placeholder="e.g. Starbucks Coffee"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            className="w-full border border-slate-200 rounded-lg px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-indigo-100 focus:border-indigo-600 transition-all shadow-sm bg-white text-slate-900"
          />
        </div>

        {/* Amount Input */}
        <div>
          <label htmlFor="amount" className="text-[11px] font-semibold text-slate-400 uppercase block mb-1">
            Amount (₹) <span className="text-rose-500">*</span>
          </label>
          <div className="relative">
            <span className="absolute left-3 top-2 text-slate-400 text-sm">₹</span>
            <input
              id="amount"
              type="number"
              step="0.01"
              required
              min="0.01"
              placeholder="0.00"
              value={amount}
              onChange={(e) => setAmount(e.target.value !== "" ? parseFloat(e.target.value) : "")}
              className="w-full border border-slate-200 rounded-lg pl-7 pr-3 py-2 text-sm outline-none focus:ring-2 focus:ring-indigo-100 focus:border-indigo-600 transition-all shadow-sm bg-white text-slate-900"
            />
          </div>
        </div>

        {/* Category Selector */}
        <div>
          <label htmlFor="category" className="text-[11px] font-semibold text-slate-400 uppercase block mb-1">
            Category <span className="text-rose-500">*</span>
          </label>
          <select
            id="category"
            value={category}
            onChange={(e) => setCategory(e.target.value)}
            className="w-full border border-slate-200 rounded-lg px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-indigo-100 focus:border-indigo-600 transition-all shadow-sm bg-white text-slate-900"
          >
            {CATEGORIES.map((cat) => (
              <option key={cat} value={cat}>
                {cat}
              </option>
            ))}
          </select>
        </div>

        {/* Date Selector */}
        <div>
          <label htmlFor="date" className="text-[11px] font-semibold text-slate-400 uppercase block mb-1">
            Date <span className="text-rose-500">*</span>
          </label>
          <input
            id="date"
            type="date"
            required
            value={date}
            onChange={(e) => setDate(e.target.value)}
            className="w-full border border-slate-200 rounded-lg px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-indigo-100 focus:border-indigo-600 transition-all shadow-sm bg-white text-slate-900"
          />
        </div>

        {/* Description Input */}
        <div>
          <label htmlFor="description" className="text-[11px] font-semibold text-slate-400 uppercase block mb-1">
            Description
          </label>
          <textarea
            id="description"
            rows={3}
            placeholder="Notes..."
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            className="w-full border border-slate-200 rounded-lg px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-indigo-100 focus:border-indigo-600 transition-all shadow-sm h-20 resize-none bg-white text-slate-900"
          />
        </div>

        {/* Form Error Message */}
        {errorMsg && (
          <div className="bg-rose-50 border border-rose-100 text-rose-700 rounded-lg p-3 text-xs" id="form-error">
            {errorMsg}
          </div>
        )}

        {/* Action Buttons */}
        <div className="flex flex-col gap-2 pt-2">
          <button
            type="submit"
            disabled={isSubmitting}
            className={`w-full text-white font-semibold py-2.5 rounded-lg transition-colors shadow-lg disabled:opacity-50 flex items-center justify-center gap-2 ${
              editingTransaction
                ? "bg-amber-500 hover:bg-amber-600 shadow-amber-100"
                : "bg-indigo-600 hover:bg-indigo-700 shadow-indigo-150"
            }`}
            id="btn-submit-form"
          >
            {isSubmitting ? (
              <RefreshCcw className="animate-spin" size={14} />
            ) : editingTransaction ? (
              <>
                <Edit3 size={14} />
                <span>Save Changes</span>
              </>
            ) : (
              <>
                <span>Save Transaction</span>
              </>
            )}
          </button>
          {editingTransaction && (
            <button
              type="button"
              onClick={onCancelEdit}
              className="w-full bg-slate-100 hover:bg-slate-200 text-slate-700 py-2 px-4 rounded-lg text-xs font-semibold transition-colors"
            >
              Cancel Edit
            </button>
          )}
        </div>
      </form>
    </div>
  );
}

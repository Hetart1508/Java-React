export interface Transaction {
  id: number;
  title: string;
  amount: number;
  type: "INCOME" | "EXPENSE";
  category: string;
  date: string;
  description: string;
}

export interface AuthUser {
  id: number;
  name: string;
  email: string;
}

export interface SummaryStats {
  totalIncome: number;
  totalExpense: number;
  currentBalance: number;
}

export interface Filters {
  type: string;
  category: string;
  date: string;
}

export type SortField = "date" | "title" | "category" | "type" | "amount";
export type SortDirection = "asc" | "desc";

export interface TransactionQuery extends Filters {
  page: number;
  pageSize: number;
  sortBy: SortField;
  sortDir: SortDirection;
}

export interface PaginatedTransactions {
  items: Transaction[];
  total: number;
  page: number;
  pageSize: number;
}

import { FormEvent, useState } from "react";
import axios from "axios";
import { LogIn, UserPlus, WalletCards } from "lucide-react";
import { AuthUser } from "../types";

interface AuthPanelProps {
  onAuthenticated: (user: AuthUser) => void;
}

type AuthMode = "login" | "signup";

export default function AuthPanel({ onAuthenticated }: AuthPanelProps) {
  const [mode, setMode] = useState<AuthMode>("login");
  const [name, setName] = useState("");
  const [email, setEmail] = useState("demo@example.com");
  const [password, setPassword] = useState("password123");
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    setError(null);
    setLoading(true);

    try {
      const endpoint = mode === "login" ? "/api/auth/login" : "/api/auth/signup";
      const payload =
        mode === "login"
          ? { email, password }
          : { name, email, password };

      const response = await axios.post<AuthUser>(endpoint, payload);
      onAuthenticated(response.data);
    } catch (err: any) {
      setError(err.response?.data?.error || err.message || "Authentication failed.");
    } finally {
      setLoading(false);
    }
  };

  const switchMode = (nextMode: AuthMode) => {
    setMode(nextMode);
    setError(null);
    if (nextMode === "signup") {
      setEmail("");
      setPassword("");
    } else {
      setEmail("demo@example.com");
      setPassword("password123");
    }
  };

  return (
    <div className="min-h-screen bg-slate-50 text-slate-900 flex items-center justify-center p-6">
      <div className="w-full max-w-md bg-white border border-slate-200 rounded-lg shadow-sm overflow-hidden">
        <div className="p-6 border-b border-slate-100">
          <div className="w-10 h-10 bg-indigo-600 rounded-lg flex items-center justify-center text-white mb-4">
            <WalletCards size={20} />
          </div>
          <h1 className="text-lg font-semibold tracking-tight text-slate-950">Finance Manager</h1>
          <p className="text-xs text-slate-500 mt-1">
            {mode === "login" ? "Log in with the demo account or your own account." : "Create an account to start a fresh session."}
          </p>
        </div>

        <div className="grid grid-cols-2 p-1 bg-slate-100 border-b border-slate-200">
          <button
            type="button"
            onClick={() => switchMode("login")}
            className={`flex items-center justify-center gap-2 text-xs font-semibold rounded-md py-2 ${
              mode === "login" ? "bg-white text-indigo-700 shadow-sm" : "text-slate-600"
            }`}
          >
            <LogIn size={14} />
            Login
          </button>
          <button
            type="button"
            onClick={() => switchMode("signup")}
            className={`flex items-center justify-center gap-2 text-xs font-semibold rounded-md py-2 ${
              mode === "signup" ? "bg-white text-indigo-700 shadow-sm" : "text-slate-600"
            }`}
          >
            <UserPlus size={14} />
            Sign up
          </button>
        </div>

        <form onSubmit={handleSubmit} className="p-6 space-y-4">
          {mode === "signup" && (
            <div>
              <label htmlFor="auth-name" className="text-[11px] font-semibold text-slate-500 uppercase block mb-1">
                Name
              </label>
              <input
                id="auth-name"
                value={name}
                onChange={(event) => setName(event.target.value)}
                className="w-full border border-slate-200 rounded-lg px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-indigo-100 focus:border-indigo-600"
                required
                minLength={2}
              />
            </div>
          )}

          <div>
            <label htmlFor="auth-email" className="text-[11px] font-semibold text-slate-500 uppercase block mb-1">
              Email
            </label>
            <input
              id="auth-email"
              type="email"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              className="w-full border border-slate-200 rounded-lg px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-indigo-100 focus:border-indigo-600"
              required
            />
          </div>

          <div>
            <label htmlFor="auth-password" className="text-[11px] font-semibold text-slate-500 uppercase block mb-1">
              Password
            </label>
            <input
              id="auth-password"
              type="password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              className="w-full border border-slate-200 rounded-lg px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-indigo-100 focus:border-indigo-600"
              required
              minLength={6}
            />
          </div>

          {error && (
            <div className="bg-rose-50 border border-rose-100 text-rose-700 rounded-lg p-3 text-xs">
              {error}
            </div>
          )}

          <button
            type="submit"
            disabled={loading}
            className="w-full bg-indigo-600 hover:bg-indigo-700 disabled:opacity-60 text-white font-semibold py-2.5 rounded-lg text-sm flex items-center justify-center gap-2"
          >
            {mode === "login" ? <LogIn size={15} /> : <UserPlus size={15} />}
            {loading ? "Please wait..." : mode === "login" ? "Login" : "Create account"}
          </button>
        </form>
      </div>
    </div>
  );
}

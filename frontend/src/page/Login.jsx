import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";

function Login() {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");

  const navigate = useNavigate();

  useEffect(() => {
    if (localStorage.getItem("isDemoAuthenticated") === "true") {
      navigate("/dashboard", { replace: true });
    }
  }, [navigate]);

  const handleLogin = (e) => {
    e.preventDefault();
    setError("");

    if (username.trim() === "admin" && password === "admin123") {
      localStorage.setItem("isDemoAuthenticated", "true");
      navigate("/dashboard", { replace: true });
    } else {
      setError("Invalid username or password");
    }
  };

  const fillDemoCredentials = () => {
    setUsername("admin");
    setPassword("admin123");
    setError("");
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-slate-900 via-slate-800 to-slate-900 p-4">
      <div className="w-full max-w-md">
        {/* Brand Header */}
        <div className="text-center mb-8">
          <div className="inline-flex items-center justify-center w-14 h-14 rounded-2xl bg-white/10 text-white text-2xl font-bold mb-4 shadow-inner border border-white/10">
            ⚙️
          </div>
          <h1 className="text-2xl font-bold text-white tracking-tight">
            Machine Shop Scheduler
          </h1>
          <p className="text-sm text-slate-400 mt-1">
            Production Planning & Dynamic Rescheduling System
          </p>
        </div>

        {/* Login Card */}
        <div className="bg-white rounded-2xl shadow-2xl border border-slate-200/80 p-8">
          <div className="mb-6">
            <h2 className="text-xl font-bold text-slate-900">
              Sign In
            </h2>
            <p className="text-xs text-slate-500 mt-1">
              Enter your credentials to access the shop floor supervisor console
            </p>
          </div>

          <form onSubmit={handleLogin} className="space-y-4">
            {/* Username Input */}
            <div>
              <label className="block text-xs font-semibold uppercase tracking-wider text-slate-600 mb-1.5">
                Username
              </label>
              <input
                type="text"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                placeholder="e.g. admin"
                className="w-full px-4 py-2.5 bg-slate-50 border border-slate-300 rounded-lg text-sm text-slate-900 outline-none focus:ring-2 focus:ring-slate-900 focus:bg-white transition"
              />
            </div>

            {/* Password Input */}
            <div>
              <label className="block text-xs font-semibold uppercase tracking-wider text-slate-600 mb-1.5">
                Password
              </label>
              <input
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="••••••••"
                className="w-full px-4 py-2.5 bg-slate-50 border border-slate-300 rounded-lg text-sm text-slate-900 outline-none focus:ring-2 focus:ring-slate-900 focus:bg-white transition"
              />
            </div>

            {/* Error Message */}
            {error && (
              <div className="p-3 bg-red-50 border border-red-200 rounded-lg text-xs font-medium text-red-700 flex items-center gap-2">
                <span>⚠️</span>
                <span>{error}</span>
              </div>
            )}

            {/* Login Button */}
            <button
              type="submit"
              className="w-full bg-slate-900 text-white py-3 rounded-lg text-sm font-semibold hover:bg-slate-800 transition shadow-md hover:shadow-lg active:scale-[0.99]"
            >
              Sign In to Console
            </button>
          </form>

          {/* Demo Credentials Box */}
          <div className="mt-6 pt-5 border-t border-slate-100">
            <div className="flex items-center justify-between mb-2">
              <span className="text-xs font-semibold text-slate-500 uppercase tracking-wider">
                Demo Access
              </span>
              <button
                type="button"
                onClick={fillDemoCredentials}
                className="text-xs font-semibold text-indigo-600 hover:text-indigo-800 hover:underline"
              >
                Auto-fill
              </button>
            </div>

            <div className="p-3 bg-slate-50 border border-slate-200 rounded-lg text-xs text-slate-700 space-y-1">
              <div className="flex justify-between">
                <span className="text-slate-500">Username:</span>
                <code className="font-semibold text-slate-900 bg-slate-200/60 px-1.5 py-0.5 rounded">admin</code>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-500">Password:</span>
                <code className="font-semibold text-slate-900 bg-slate-200/60 px-1.5 py-0.5 rounded">admin123</code>
              </div>
            </div>
          </div>
        </div>

        {/* Footer */}
        <p className="text-center text-xs text-slate-500 mt-6">
          Machine Shop Scheduler &copy; {new Date().getFullYear()} &bull; Demo Mode
        </p>
      </div>
    </div>
  );
}

export default Login;
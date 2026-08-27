import { useState } from "react";
import { useNavigate } from "react-router-dom";

function Login() {

  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");

  const navigate = useNavigate();

  const handleLogin = (e) => {

    e.preventDefault();

    // Demo credentials
    if (username === "admin" && password === "admin123") {
      navigate("/dashboard");
    } else {
      setError("Invalid username or password");
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-slate-100">

      <div className="w-full max-w-md">

        {/* Logo / Title */}
        <div className="text-center mb-8">

          <h1 className="text-3xl font-bold text-slate-900">
            Machine Shop
          </h1>

          <p className="mt-2 text-slate-500">
            Production Scheduling System
          </p>

        </div>

        {/* Login Card */}
        <div className="bg-white rounded-xl shadow-lg border border-slate-200 p-8">

          <h2 className="text-2xl font-semibold text-slate-900 mb-6">
            Sign in
          </h2>

          <form onSubmit={handleLogin}>

            {/* Username */}
            <div className="mb-5">

              <label className="block text-sm font-medium text-slate-700 mb-2">
                Username
              </label>

              <input
                type="text"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                placeholder="Enter username"
                className="w-full px-4 py-3 border border-slate-300 rounded-lg outline-none focus:ring-2 focus:ring-slate-400"
              />

            </div>

            {/* Password */}
            <div className="mb-5">

              <label className="block text-sm font-medium text-slate-700 mb-2">
                Password
              </label>

              <input
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="Enter password"
                className="w-full px-4 py-3 border border-slate-300 rounded-lg outline-none focus:ring-2 focus:ring-slate-400"
              />

            </div>

            {/* Error */}
            {error && (
              <p className="text-sm text-red-600 mb-4">
                {error}
              </p>
            )}

            {/* Login Button */}
            <button
              type="submit"
              className="w-full bg-slate-900 text-white py-3 rounded-lg font-medium hover:bg-slate-800 transition"
            >
              Login
            </button>

          </form>

          {/* Demo credentials */}
          <div className="mt-6 p-4 bg-slate-50 rounded-lg">

            <p className="text-xs text-slate-500">
              Demo credentials
            </p>

            <p className="text-sm text-slate-700 mt-1">
              Username: <strong>admin</strong>
            </p>

            <p className="text-sm text-slate-700">
              Password: <strong>admin123</strong>
            </p>

          </div>

        </div>

      </div>

    </div>
  );
}

export default Login;
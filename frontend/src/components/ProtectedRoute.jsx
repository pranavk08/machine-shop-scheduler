import { Navigate, Outlet } from "react-router-dom";
import Sidebar from "./Sidebar";

function ProtectedRoute() {
  const isAuthenticated = localStorage.getItem("isDemoAuthenticated") === "true";

  if (!isAuthenticated) {
    return <Navigate to="/" replace />;
  }

  return (
    <div className="flex min-h-screen bg-slate-100">
      <Sidebar />
      <main className="flex-1 p-8 overflow-y-auto">
        <Outlet />
      </main>
    </div>
  );
}

export default ProtectedRoute;

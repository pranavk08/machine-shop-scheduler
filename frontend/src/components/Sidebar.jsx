import { NavLink, useNavigate } from "react-router-dom";

function Sidebar() {
  const navigate = useNavigate();

  const menuItems = [
    { name: "Dashboard", path: "/dashboard", icon: "📊" },
    { name: "Orders", path: "/orders", icon: "📦" },
    { name: "Schedule", path: "/schedule", icon: "📅" },
    { name: "Machines", path: "/machines", icon: "🏭" },
    { name: "Operators", path: "/operators", icon: "👷" },
    { name: "Disruptions", path: "/disruptions", icon: "⚡" },
  ];

  const handleLogout = () => {
    localStorage.removeItem("isDemoAuthenticated");
    navigate("/", { replace: true });
  };

  return (
    <aside className="w-64 min-h-screen bg-slate-900 text-white p-5 flex flex-col justify-between border-r border-slate-800 shadow-xl">
      <div>
        {/* Brand Header */}
        <div className="flex items-center gap-3 px-2 mb-8">
          <div className="w-9 h-9 rounded-lg bg-indigo-600 flex items-center justify-center text-lg font-bold shadow-md">
            ⚙️
          </div>
          <div>
            <h1 className="text-base font-bold text-white tracking-tight">
              Machine Shop
            </h1>
            <p className="text-[11px] text-slate-400 font-medium">
              Scheduler Console
            </p>
          </div>
        </div>

        {/* Navigation Items */}
        <nav className="space-y-1.5">
          {menuItems.map((item) => (
            <NavLink
              key={item.path}
              to={item.path}
              className={({ isActive }) =>
                `flex items-center gap-3 px-3.5 py-2.5 rounded-lg text-sm font-medium transition ${
                  isActive
                    ? "bg-indigo-600 text-white shadow-sm font-semibold"
                    : "text-slate-300 hover:bg-slate-800 hover:text-white"
                }`
              }
            >
              <span className="text-base">{item.icon}</span>
              <span>{item.name}</span>
            </NavLink>
          ))}
        </nav>
      </div>

      {/* User Profile & Logout Section */}
      <div className="pt-5 border-t border-slate-800 space-y-3">
        <div className="px-3 py-2 bg-slate-800/60 rounded-lg border border-slate-700/50 flex items-center gap-2.5">
          <div className="w-7 h-7 rounded-full bg-slate-700 text-white flex items-center justify-center text-xs font-bold">
            👤
          </div>
          <div className="overflow-hidden">
            <p className="text-xs font-semibold text-white truncate">Admin Supervisor</p>
            <p className="text-[10px] text-amber-400 font-medium tracking-wide uppercase">Demo Mode</p>
          </div>
        </div>

        <button
          onClick={handleLogout}
          className="w-full flex items-center justify-center gap-2 px-3 py-2 rounded-lg text-xs font-semibold text-slate-300 hover:bg-red-900/40 hover:text-red-300 transition border border-transparent hover:border-red-800/50"
        >
          <span>🚪</span>
          <span>Sign Out</span>
        </button>
      </div>
    </aside>
  );
}

export default Sidebar;

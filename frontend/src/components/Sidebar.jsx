import { NavLink } from "react-router-dom";

function Sidebar() {
  const menuItems = [
    { name: "Dashboard", path: "/dashboard" },
    { name: "Orders", path: "/orders" },
    { name: "Schedule", path: "/schedule" },
    { name: "Machines", path: "/machines" },
    { name: "Operators", path: "/operators" },
    { name: "Disruptions", path: "/disruptions" },
  ];

  return (
    <aside className="w-56 min-h-screen bg-slate-900 text-white p-4">

      <h1 className="text-lg font-bold mb-8">
        Machine Shop
      </h1>

      <nav className="space-y-2">

        {menuItems.map((item) => (
          <NavLink
            key={item.path}
            to={item.path}
            className={({ isActive }) =>
              `block px-4 py-3 rounded-lg transition ${
                isActive
                  ? "bg-slate-700 text-white"
                  : "text-slate-200 hover:bg-slate-800"
              }`
            }
          >
            {item.name}
          </NavLink>
        ))}

      </nav>

    </aside>
  );
}

export default Sidebar;




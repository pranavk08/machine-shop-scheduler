import { BrowserRouter, Routes, Route } from "react-router-dom";
import Sidebar from "./components/Sidebar";
import Dashboard from "./page/Dashboard";
import Orders from "./page/Orders";
import Schedule from "./page/Schedule";
import Machines from "./page/Machines";
import Operators from "./page/Operators";
import Disruptions from "./page/Disruptions";
import Login from "./page/Login";




function App() {
  return (
    <BrowserRouter>
      <div className="flex min-h-screen bg-slate-100">

        <Sidebar />

        <main className="flex-1 p-8">
          <Routes>

            <Route
               path="/"
              element={<Login />}
            />

            <Route
              path="/dashboard"
              element={<Dashboard />}
            />

            <Route
              path="/orders"
              element={<Orders />}
            />

            <Route
              path="/schedule"
              element={<Schedule />}
            />

            <Route
              path="/machines"
              element={<Machines />}
            />

            <Route
               path="/operators"
                 element={<Operators />}
              />

              <Route
                path="/disruptions"
                element={<Disruptions />}
              />

          </Routes>
        </main>

      </div>
    </BrowserRouter>
  );
}

export default App;
import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import ProtectedRoute from "./components/ProtectedRoute";
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
      <Routes>
        {/* Public Route */}
        <Route path="/" element={<Login />} />

        {/* Protected Routes Layout */}
        <Route element={<ProtectedRoute />}>
          <Route path="/dashboard" element={<Dashboard />} />
          <Route path="/orders" element={<Orders />} />
          <Route path="/schedule" element={<Schedule />} />
          <Route path="/machines" element={<Machines />} />
          <Route path="/operators" element={<Operators />} />
          <Route path="/disruptions" element={<Disruptions />} />
        </Route>

        {/* Fallback to root */}
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
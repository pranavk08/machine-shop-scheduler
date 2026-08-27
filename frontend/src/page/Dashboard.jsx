import { useEffect, useState } from "react";
import api from "../services/api";

function Dashboard() {

  const [machines, setMachines] = useState([]);
  const [openOrders, setOpenOrders] = useState([]);
  const [operators, setOperators] = useState([]);


  useEffect(() => {

  // Fetch TURNING machines
  api.get("/api/scheduler/machines/TURNING")
    .then((response) => {
      console.log("Machines from backend:", response.data);
      setMachines(response.data);
    })
    .catch((error) => {
      console.error("Failed to fetch machines:", error);
    });

  // Fetch open orders
  api.get("/api/orders/open")
    .then((response) => {
      console.log("Open orders from backend:", response.data);
      setOpenOrders(response.data);
    })
    .catch((error) => {
      console.error("Failed to fetch open orders:", error);
    });

  // Fetch operators
  api.get("/api/operators")
    .then((response) => {
      console.log("Operators from backend:", response.data);
      setOperators(response.data);
    })
    .catch((error) => {
      console.error("Failed to fetch operators:", error);
    });

}, []);

  return (
    <div>

      {/* Header */}
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-slate-900">
          Dashboard
        </h1>

        <p className="mt-2 text-slate-500">
          Machine shop scheduling overview
        </p>
      </div>

      {/* Statistics */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-5">

        {/* Open Orders */}
        <div className="bg-white rounded-xl p-6 shadow-sm border border-slate-200">
          <p className="text-sm text-slate-500">
            Open Orders
          </p>

          <h2 className="text-3xl font-bold text-slate-900 mt-2">
            {openOrders.length}
          </h2>

          <p className="text-sm text-slate-400 mt-2">
            Orders awaiting production
          </p>
        </div>

        {/* Machines */}
        <div className="bg-white rounded-xl p-6 shadow-sm border border-slate-200">
          <p className="text-sm text-slate-500">
            Machines
          </p>

          <h2 className="text-3xl font-bold text-slate-900 mt-2">
            {machines.length}
          </h2>

          <p className="text-sm text-green-600 mt-2">
            TURNING machines
          </p>
        </div>

        {/* Operators */}
        <div className="bg-white rounded-xl p-6 shadow-sm border border-slate-200">
          <p className="text-sm text-slate-500">
            Operators
          </p>

          <h2 className="text-3xl font-bold text-slate-900 mt-2">
            {operators.length}
          </h2>

          <p className="text-sm text-green-600 mt-2">
            Active workforce
          </p>
        </div>

        {/* On-Time Rate */}
        <div className="bg-white rounded-xl p-6 shadow-sm border border-slate-200">
          <p className="text-sm text-slate-500">
            On-Time Rate
          </p>

          <h2 className="text-3xl font-bold text-slate-900 mt-2">
            88%
          </h2>

          <p className="text-sm text-slate-400 mt-2">
            Current schedule
          </p>
        </div>

      </div>

      {/* Bottom Section */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-5 mt-6">

        {/* Production Status */}
        <div className="bg-white rounded-xl p-6 shadow-sm border border-slate-200">

          <h2 className="text-lg font-semibold text-slate-900">
            Production Status
          </h2>

          <div className="mt-6 space-y-4">

            {/* Scheduled */}
            <div>
              <div className="flex justify-between text-sm mb-2">
                <span>Scheduled</span>
                <span>72%</span>
              </div>

              <div className="h-2 bg-slate-100 rounded-full">
                <div className="h-2 bg-slate-800 rounded-full w-[72%]"></div>
              </div>
            </div>

            {/* Completed */}
            <div>
              <div className="flex justify-between text-sm mb-2">
                <span>Completed</span>
                <span>18%</span>
              </div>

              <div className="h-2 bg-slate-100 rounded-full">
                <div className="h-2 bg-green-500 rounded-full w-[18%]"></div>
              </div>
            </div>

            {/* Delayed */}
            <div>
              <div className="flex justify-between text-sm mb-2">
                <span>Delayed</span>
                <span>10%</span>
              </div>

              <div className="h-2 bg-slate-100 rounded-full">
                <div className="h-2 bg-red-500 rounded-full w-[10%]"></div>
              </div>
            </div>

          </div>

        </div>

        {/* Machine Status */}
        <div className="bg-white rounded-xl p-6 shadow-sm border border-slate-200">

          <h2 className="text-lg font-semibold text-slate-900">
            Machine Status
          </h2>

          <div className="mt-5 space-y-3">

            {machines.map((machine) => (

              <div
                key={machine.id}
                className="flex justify-between items-center"
              >

                <span className="font-medium">
                  {machine.machineCode}
                </span>

                <span
                  className={
                    machine.available
                      ? "text-sm text-green-600"
                      : "text-sm text-red-600"
                  }
                >
                  {machine.available
                    ? "Available"
                    : "Unavailable"}
                </span>

              </div>

            ))}

          </div>

        </div>

      </div>

    </div>
  );
}

export default Dashboard;
import { useEffect, useState } from "react";
import api from "../services/api";

function Machines() {

  const [machines, setMachines] = useState([]);

  useEffect(() => {

    api.get("/api/machines")
      .then((response) => {
        console.log("Machines page data:", response.data);
        setMachines(response.data);
      })
      .catch((error) => {
        console.error("Failed to fetch machines:", error);
      });

  }, []);

  return (
    <div>

      {/* Header */}
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-slate-900">
          Machines
        </h1>

        <p className="mt-2 text-slate-500">
          Monitor machine availability and status
        </p>
      </div>

      {/* Summary */}
      <div className="bg-white rounded-xl p-6 shadow-sm border border-slate-200 mb-6">

        <p className="text-sm text-slate-500">
          Total Machines
        </p>

        <h2 className="text-3xl font-bold text-slate-900 mt-2">
          {machines.length}
        </h2>

        <p className="text-sm text-green-600 mt-2">
          Machines in workshop
        </p>

      </div>

      {/* Machines Table */}
      <div className="bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden">

        <div className="overflow-x-auto">

          <table className="w-full text-left">

            <thead className="bg-slate-50 border-b border-slate-200">

              <tr>

                <th className="px-6 py-4 text-sm font-semibold text-slate-600">
                  Machine Code
                </th>

                <th className="px-6 py-4 text-sm font-semibold text-slate-600">
                  Name
                </th>

                <th className="px-6 py-4 text-sm font-semibold text-slate-600">
                  Type
                </th>

                <th className="px-6 py-4 text-sm font-semibold text-slate-600">
                  Status
                </th>

              </tr>

            </thead>

            <tbody>

              {machines.map((machine) => (

                <tr
                  key={machine.id}
                  className="border-b border-slate-100 hover:bg-slate-50"
                >

                  <td className="px-6 py-4 font-medium text-slate-900">
                    {machine.machineCode}
                  </td>

                  <td className="px-6 py-4 text-slate-600">
                    {machine.name}
                  </td>

                  <td className="px-6 py-4 text-slate-600">
                    {machine.type}
                  </td>

                  <td className="px-6 py-4">

                    <span
                      className={
                        machine.available
                          ? "px-3 py-1 rounded-full text-xs font-medium bg-green-100 text-green-700"
                          : "px-3 py-1 rounded-full text-xs font-medium bg-red-100 text-red-700"
                      }
                    >
                      {machine.available
                        ? "Available"
                        : "Unavailable"}
                    </span>

                  </td>

                </tr>

              ))}

            </tbody>

          </table>

        </div>

        {machines.length === 0 && (
          <div className="p-8 text-center text-slate-500">
            No machines found.
          </div>
        )}

      </div>

    </div>
  );
}

export default Machines;
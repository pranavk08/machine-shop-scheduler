import { useEffect, useState } from "react";
import api from "../services/api";

function Disruptions() {

  const [breakdowns, setBreakdowns] = useState([]);

  useEffect(() => {

    api.get("/api/breakdowns")
      .then((response) => {
        console.log("Breakdowns from backend:", response.data);
        setBreakdowns(response.data);
      })
      .catch((error) => {
        console.error("Failed to fetch breakdowns:", error);
      });

  }, []);

  return (
    <div>

      {/* Header */}
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-slate-900">
          Disruptions
        </h1>

        <p className="mt-2 text-slate-500">
          Monitor machine breakdowns and production disruptions
        </p>
      </div>

      {/* Summary */}
      <div className="bg-white rounded-xl p-6 shadow-sm border border-slate-200 mb-6">

        <p className="text-sm text-slate-500">
          Total Breakdowns
        </p>

        <h2 className="text-3xl font-bold text-slate-900 mt-2">
          {breakdowns.length}
        </h2>

      </div>

      {/* Breakdowns Table */}
      <div className="bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden">

        <div className="overflow-x-auto">

          <table className="w-full text-left">

            <thead className="bg-slate-50 border-b border-slate-200">

              <tr>

                <th className="px-6 py-4 text-sm font-semibold text-slate-600">
                  Breakdown ID
                </th>

                <th className="px-6 py-4 text-sm font-semibold text-slate-600">
                  Machine
                </th>

                <th className="px-6 py-4 text-sm font-semibold text-slate-600">
                  Start Time
                </th>

                <th className="px-6 py-4 text-sm font-semibold text-slate-600">
                  End Time
                </th>

              </tr>

            </thead>

            <tbody>

              {breakdowns.map((breakdown) => (

                <tr
                  key={breakdown.id}
                  className="border-b border-slate-100 hover:bg-slate-50"
                >

                  <td className="px-6 py-4 font-medium text-slate-900">
                    {breakdown.id}
                  </td>

                  <td className="px-6 py-4 text-slate-600">
                    {breakdown.machine?.machineCode || "-"}
                  </td>

                  <td className="px-6 py-4 text-slate-600">
                    {breakdown.startTime
                      ? new Date(breakdown.startTime).toLocaleString()
                      : "-"}
                  </td>

                  <td className="px-6 py-4 text-slate-600">
                    {breakdown.endTime
                      ? new Date(breakdown.endTime).toLocaleString()
                      : "-"}
                  </td>

                </tr>

              ))}

            </tbody>

          </table>

        </div>

        {breakdowns.length === 0 && (
          <div className="p-8 text-center text-slate-500">
            No breakdowns found.
          </div>
        )}

      </div>

    </div>
  );
}

export default Disruptions;
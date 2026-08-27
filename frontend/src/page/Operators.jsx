import { useEffect, useState } from "react";
import api from "../services/api";

function Operators() {

  const [operators, setOperators] = useState([]);

  useEffect(() => {

    api.get("/api/operators")
      .then((response) => {
        console.log("Operators page data:", response.data);
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
          Operators
        </h1>

        <p className="mt-2 text-slate-500">
          Monitor operator availability and skills
        </p>
      </div>

      {/* Operators Table */}
      <div className="bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden">

        <div className="overflow-x-auto">

          <table className="w-full text-left">

            <thead className="bg-slate-50 border-b border-slate-200">

              <tr>

                <th className="px-6 py-4 text-sm font-semibold text-slate-600">
                  Operator Code
                </th>

                <th className="px-6 py-4 text-sm font-semibold text-slate-600">
                  Name
                </th>

                <th className="px-6 py-4 text-sm font-semibold text-slate-600">
                  Status
                </th>

              </tr>

            </thead>

            <tbody>

              {operators.map((operator) => (

                <tr
                  key={operator.id}
                  className="border-b border-slate-100 hover:bg-slate-50"
                >

                  <td className="px-6 py-4 font-medium text-slate-900">
                    {operator.operatorCode}
                  </td>

                  <td className="px-6 py-4 text-slate-600">
                    {operator.name}
                  </td>

                  <td className="px-6 py-4">

                    <span
                      className={
                        operator.available
                          ? "px-3 py-1 rounded-full text-xs font-medium bg-green-100 text-green-700"
                          : "px-3 py-1 rounded-full text-xs font-medium bg-red-100 text-red-700"
                      }
                    >
                      {operator.available
                        ? "Available"
                        : "Unavailable"}
                    </span>

                  </td>

                </tr>

              ))}

            </tbody>

          </table>

        </div>

        {operators.length === 0 && (
          <div className="p-8 text-center text-slate-500">
            No operators found.
          </div>
        )}

      </div>

    </div>
  );
}

export default Operators;
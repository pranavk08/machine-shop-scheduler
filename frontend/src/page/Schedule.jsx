import { useEffect, useState } from "react";
import api from "../services/api";

function Schedule() {

  const [schedule, setSchedule] = useState([]);

  useEffect(() => {

    api.get("/api/scheduler/orders/schedule")
      .then((response) => {
        console.log("Schedule from backend:", response.data);
        setSchedule(response.data);
      })
      .catch((error) => {
        console.error("Failed to fetch schedule:", error);
      });

  }, []);

  return (
    <div>

      {/* Header */}
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-slate-900">
          Production Schedule
        </h1>

        <p className="mt-2 text-slate-500">
          Machine and operator production schedule
        </p>
      </div>

      {/* Schedule Table */}
      <div className="bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden">

        <div className="overflow-x-auto">

          <table className="w-full text-left">

            <thead className="bg-slate-50 border-b border-slate-200">

              <tr>

                <th className="px-6 py-4 text-sm font-semibold text-slate-600">
                  Order
                </th>

                <th className="px-6 py-4 text-sm font-semibold text-slate-600">
                  Operation
                </th>

                <th className="px-6 py-4 text-sm font-semibold text-slate-600">
                  Sequence
                </th>

                <th className="px-6 py-4 text-sm font-semibold text-slate-600">
                  Machine
                </th>

                <th className="px-6 py-4 text-sm font-semibold text-slate-600">
                  Operator
                </th>

                <th className="px-6 py-4 text-sm font-semibold text-slate-600">
                  Start
                </th>

                <th className="px-6 py-4 text-sm font-semibold text-slate-600">
                  End
                </th>

              </tr>

            </thead>

            <tbody>

              {schedule.map((item, index) => (

                <tr
                  key={index}
                  className="border-b border-slate-100 hover:bg-slate-50"
                >

                  {/* Order */}
                  <td className="px-6 py-4 font-medium text-slate-900">
                    {item.operation?.order?.orderNumber || "-"}
                  </td>

                  {/* Operation */}
                  <td className="px-6 py-4 text-slate-600">
                    {item.operation?.operationType || "-"}
                  </td>

                  {/* Sequence */}
                  <td className="px-6 py-4 text-slate-600">
                    {item.operation?.sequenceNumber || "-"}
                  </td>

                  {/* Machine */}
                  <td className="px-6 py-4 text-slate-600">
                    {item.machine?.machineCode || "-"}
                  </td>

                  {/* Operator */}
                  <td className="px-6 py-4 text-slate-600">
                    {item.operator?.name || "-"}
                  </td>

                  {/* Start */}
                  <td className="px-6 py-4 text-slate-600">
                    {item.startTime
                      ? new Date(item.startTime).toLocaleString()
                      : "-"}
                  </td>

                  {/* End */}
                  <td className="px-6 py-4 text-slate-600">
                    {item.endTime
                      ? new Date(item.endTime).toLocaleString()
                      : "-"}
                  </td>

                </tr>

              ))}

            </tbody>

          </table>

        </div>

        {schedule.length === 0 && (
          <div className="p-8 text-center text-slate-500">
            No schedule data found.
          </div>
        )}

      </div>

    </div>
  );
}

export default Schedule;
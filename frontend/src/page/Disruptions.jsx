import { useEffect, useState } from "react";
import api from "../services/api";

function Disruptions() {
  const [breakdowns, setBreakdowns] = useState([]);
  const [machines, setMachines] = useState([]);
  const [loading, setLoading] = useState(false);
  const [replanLoading, setReplanLoading] = useState(false);
  const [showModal, setShowModal] = useState(false);
  const [replanResult, setReplanResult] = useState(null);
  const [errorMessage, setErrorMessage] = useState("");
  const [formErrors, setFormErrors] = useState({});

  // Form state
  const [formData, setFormData] = useState({
    machineId: "",
    startTime: "",
    endTime: "",
    reason: "",
  });

  const fetchBreakdowns = () => {
    api.get("/api/breakdowns")
      .then((response) => {
        setBreakdowns(response.data);
      })
      .catch((error) => {
        console.error("Failed to fetch breakdowns:", error);
      });
  };

  const fetchMachines = () => {
    api.get("/api/machines")
      .then((response) => {
        setMachines(response.data);
      })
      .catch((error) => {
        console.error("Failed to fetch machines:", error);
      });
  };

  useEffect(() => {
    fetchBreakdowns();
    fetchMachines();
  }, []);

  const handleOpenModal = () => {
    const now = new Date();
    const future = new Date(now.getTime() + 4 * 60 * 60 * 1000);
    const formatLocal = (d) => d.toISOString().slice(0, 16);

    setFormData({
      machineId: machines.length > 0 ? machines[0].id : "",
      startTime: formatLocal(now),
      endTime: formatLocal(future),
      reason: "",
    });
    setFormErrors({});
    setErrorMessage("");
    setShowModal(true);
  };

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  const handleSubmitBreakdown = (e) => {
    e.preventDefault();
    setErrorMessage("");
    setFormErrors({});

    if (!formData.machineId) {
      setErrorMessage("Please select a machine.");
      return;
    }
    if (!formData.startTime || !formData.endTime) {
      setErrorMessage("Start time and end time are required.");
      return;
    }
    if (new Date(formData.endTime) <= new Date(formData.startTime)) {
      setErrorMessage("End time must be after start time.");
      return;
    }
    if (!formData.reason.trim()) {
      setErrorMessage("Reason is required.");
      return;
    }

    setLoading(true);
    api.post("/api/breakdowns", {
      machineId: Number(formData.machineId),
      startTime: formData.startTime,
      endTime: formData.endTime,
      reason: formData.reason.trim(),
    })
      .then(() => {
        setShowModal(false);
        fetchBreakdowns();
      })
      .catch((err) => {
        const data = err.response?.data;
        if (data?.fieldErrors) {
          setFormErrors(data.fieldErrors);
        }
        setErrorMessage(data?.message || "Failed to create breakdown.");
      })
      .finally(() => {
        setLoading(false);
      });
  };

  const handleTriggerReplan = () => {
    setReplanLoading(true);
    setErrorMessage("");
    api.post("/api/scheduler/replan", {})
      .then((response) => {
        setReplanResult(response.data);
      })
      .catch((err) => {
        console.error("Replanning failed:", err);
        setErrorMessage(err.response?.data?.message || "Replanning failed.");
      })
      .finally(() => {
        setReplanLoading(false);
      });
  };

  const formatTimeSlot = (startStr, endStr) => {
    if (!startStr || !endStr) return "-";
    const start = new Date(startStr);
    const end = new Date(endStr);

    const isSameDate = start.toDateString() === end.toDateString();
    const startTimeFormatted = start.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    const endTimeFormatted = end.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    const dateStr = start.toLocaleDateString([], { month: 'short', day: 'numeric' });

    if (isSameDate) {
      return `${dateStr}, ${startTimeFormatted} - ${endTimeFormatted}`;
    }
    const endDateStr = end.toLocaleDateString([], { month: 'short', day: 'numeric' });
    return `${dateStr} ${startTimeFormatted} - ${endDateStr} ${endTimeFormatted}`;
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h1 className="text-3xl font-bold text-slate-900">Disruptions & Cost Analysis</h1>
          <p className="mt-1 text-slate-500">
            Monitor machine breakdowns, dynamic replanning, overtime impact, and late-order penalty exposure
          </p>
        </div>

        <div className="flex gap-3">
          <button
            onClick={handleOpenModal}
            className="px-4 py-2.5 bg-red-600 text-white font-medium rounded-lg hover:bg-red-700 transition shadow-sm"
          >
            + Report Breakdown
          </button>
          <button
            onClick={handleTriggerReplan}
            disabled={replanLoading}
            className="px-4 py-2.5 bg-slate-900 text-white font-medium rounded-lg hover:bg-slate-800 transition disabled:opacity-50 shadow-sm"
          >
            {replanLoading ? "Replanning..." : "⚡ Replan Schedule"}
          </button>
        </div>
      </div>

      {errorMessage && (
        <div className="p-4 bg-red-50 border border-red-200 rounded-xl text-red-700 text-sm">
          {errorMessage}
        </div>
      )}

      {/* Operational KPI Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-4">
        <div className="bg-white rounded-xl p-5 shadow-sm border border-slate-200">
          <p className="text-xs font-semibold uppercase tracking-wider text-slate-500">Total Breakdowns</p>
          <h2 className="text-3xl font-bold text-slate-900 mt-2">{breakdowns.length}</h2>
          <p className="text-xs text-slate-400 mt-1">Logged downtime events</p>
        </div>

        {replanResult && (
          <>
            <div className="bg-white rounded-xl p-5 shadow-sm border border-slate-200">
              <p className="text-xs font-semibold uppercase tracking-wider text-slate-500">Operations Shifted</p>
              <h2 className="text-3xl font-bold text-amber-600 mt-2">
                {replanResult.operationsMovedCount}
              </h2>
              <p className="text-xs text-slate-400 mt-1">
                Out of {replanResult.totalOperations} operations
              </p>
            </div>

            <div className="bg-white rounded-xl p-5 shadow-sm border border-slate-200">
              <p className="text-xs font-semibold uppercase tracking-wider text-slate-500">Machines Reassigned</p>
              <h2 className="text-3xl font-bold text-blue-600 mt-2">
                {replanResult.machinesReassignedCount}
              </h2>
              <p className="text-xs text-slate-400 mt-1">Rerouted machines</p>
            </div>

            <div className="bg-white rounded-xl p-5 shadow-sm border border-slate-200">
              <p className="text-xs font-semibold uppercase tracking-wider text-slate-500">Operators Reassigned</p>
              <h2 className="text-3xl font-bold text-indigo-600 mt-2">
                {replanResult.operatorsReassignedCount}
              </h2>
              <p className="text-xs text-slate-400 mt-1">Reassigned staff</p>
            </div>

            <div className="bg-white rounded-xl p-5 shadow-sm border border-slate-200">
              <p className="text-xs font-semibold uppercase tracking-wider text-slate-500">Orders Delayed</p>
              <h2 className="text-3xl font-bold text-red-600 mt-2">
                {replanResult.ordersDelayedCount}
              </h2>
              <p className="text-xs text-slate-400 mt-1">Delayed completions</p>
            </div>
          </>
        )}
      </div>

      {/* Financial Cost Impact Section */}
      {replanResult && replanResult.afterCostSummary && (
        <div className="space-y-4">
          <div className="flex items-center justify-between">
            <h2 className="text-xl font-bold text-slate-900">💰 Financial Impact Analysis</h2>
            <div className="flex items-center gap-2">
              <span className="text-xs font-medium text-slate-500">Net Cost Impact:</span>
              <span className={`px-3 py-1 text-xs font-bold rounded-full ${
                replanResult.netCostImpact > 0
                  ? "bg-red-100 text-red-800"
                  : replanResult.netCostImpact < 0
                  ? "bg-green-100 text-green-800"
                  : "bg-slate-100 text-slate-700"
              }`}>
                {replanResult.netCostImpact > 0 ? "+" : ""}₹{replanResult.netCostImpact.toLocaleString()}
              </span>
            </div>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-5">
            {/* Overtime Cost Card */}
            <div className="bg-white rounded-xl p-6 shadow-sm border border-slate-200">
              <div className="flex justify-between items-start">
                <div>
                  <p className="text-xs font-semibold uppercase tracking-wider text-slate-500">Overtime Cost</p>
                  <h3 className="text-2xl font-bold text-slate-900 mt-1">
                    ₹{replanResult.afterCostSummary.totalOvertimeCost.toLocaleString()}
                  </h3>
                </div>
                <span className="px-2.5 py-1 text-xs font-medium rounded-md bg-amber-50 text-amber-700 border border-amber-200">
                  {replanResult.afterCostSummary.totalOvertimeHours} hrs OT
                </span>
              </div>
              <div className="mt-4 pt-3 border-t border-slate-100 flex justify-between items-center text-xs text-slate-500">
                <span>Baseline: ₹{replanResult.beforeCostSummary?.totalOvertimeCost.toLocaleString() || 0}</span>
                <span className={replanResult.afterCostSummary.totalOvertimeCost > (replanResult.beforeCostSummary?.totalOvertimeCost || 0) ? "text-red-600 font-semibold" : "text-green-600"}>
                  {replanResult.afterCostSummary.totalOvertimeCost >= (replanResult.beforeCostSummary?.totalOvertimeCost || 0) ? "+" : ""}
                  ₹{(replanResult.afterCostSummary.totalOvertimeCost - (replanResult.beforeCostSummary?.totalOvertimeCost || 0)).toLocaleString()}
                </span>
              </div>
            </div>

            {/* Late Delivery Penalty Card */}
            <div className="bg-white rounded-xl p-6 shadow-sm border border-slate-200">
              <div className="flex justify-between items-start">
                <div>
                  <p className="text-xs font-semibold uppercase tracking-wider text-slate-500">Late Penalty Exposure</p>
                  <h3 className="text-2xl font-bold text-red-600 mt-1">
                    ₹{replanResult.afterCostSummary.totalPenaltyCost.toLocaleString()}
                  </h3>
                </div>
                <span className="px-2.5 py-1 text-xs font-medium rounded-md bg-red-50 text-red-700 border border-red-200">
                  {replanResult.afterCostSummary.lateOrdersCount} Late Orders
                </span>
              </div>
              <div className="mt-4 pt-3 border-t border-slate-100 flex justify-between items-center text-xs text-slate-500">
                <span>Baseline: ₹{replanResult.beforeCostSummary?.totalPenaltyCost.toLocaleString() || 0}</span>
                <span className={replanResult.afterCostSummary.totalPenaltyCost > (replanResult.beforeCostSummary?.totalPenaltyCost || 0) ? "text-red-600 font-semibold" : "text-green-600"}>
                  {replanResult.afterCostSummary.totalPenaltyCost >= (replanResult.beforeCostSummary?.totalPenaltyCost || 0) ? "+" : ""}
                  ₹{(replanResult.afterCostSummary.totalPenaltyCost - (replanResult.beforeCostSummary?.totalPenaltyCost || 0)).toLocaleString()}
                </span>
              </div>
            </div>

            {/* Total Disruption Cost Card */}
            <div className="bg-white rounded-xl p-6 shadow-sm border border-slate-200 bg-gradient-to-br from-slate-900 to-slate-800 text-white">
              <div className="flex justify-between items-start">
                <div>
                  <p className="text-xs font-semibold uppercase tracking-wider text-slate-300">Total Production Cost</p>
                  <h3 className="text-3xl font-bold text-white mt-1">
                    ₹{replanResult.afterCostSummary.totalCost.toLocaleString()}
                  </h3>
                </div>
                <span className="px-2.5 py-1 text-xs font-bold rounded-md bg-white/20 text-white">
                  After Replan
                </span>
              </div>
              <div className="mt-4 pt-3 border-t border-slate-700/60 flex justify-between items-center text-xs text-slate-300">
                <span>Baseline: ₹{replanResult.beforeCostSummary?.totalCost.toLocaleString() || 0}</span>
                <span className="text-amber-300 font-bold">
                  Net Δ: {replanResult.netCostImpact >= 0 ? "+" : ""}₹{replanResult.netCostImpact.toLocaleString()}
                </span>
              </div>
            </div>
          </div>

          {/* Late Orders Penalty Breakdown Table */}
          {replanResult.afterCostSummary.lateOrders && replanResult.afterCostSummary.lateOrders.length > 0 && (
            <div className="bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden">
              <div className="px-6 py-4 border-b border-slate-200 bg-red-50/50 flex justify-between items-center">
                <div>
                  <h3 className="text-base font-bold text-slate-900">
                    ⚠️ Late Orders & Penalty Exposure
                  </h3>
                  <p className="text-xs text-slate-500">
                    Orders projected to finish after their delivery due date
                  </p>
                </div>
                <span className="px-3 py-1 bg-red-100 text-red-800 text-xs font-semibold rounded-full">
                  {replanResult.afterCostSummary.lateOrders.length} Late Orders
                </span>
              </div>

              <div className="overflow-x-auto">
                <table className="w-full text-left text-sm">
                  <thead className="bg-slate-50 border-b border-slate-200 text-slate-600 font-semibold">
                    <tr>
                      <th className="px-6 py-3.5">Order</th>
                      <th className="px-6 py-3.5">Customer</th>
                      <th className="px-6 py-3.5">Tier</th>
                      <th className="px-6 py-3.5">Due Date</th>
                      <th className="px-6 py-3.5">Completion Date</th>
                      <th className="px-6 py-3.5">Lateness</th>
                      <th className="px-6 py-3.5">Penalty Rate</th>
                      <th className="px-6 py-3.5">Penalty Amount</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100">
                    {replanResult.afterCostSummary.lateOrders.map((late, idx) => (
                      <tr key={idx} className="hover:bg-slate-50">
                        <td className="px-6 py-4 font-semibold text-slate-900">
                          {late.orderNumber}
                        </td>
                        <td className="px-6 py-4 text-slate-700">
                          {late.customerName}
                        </td>
                        <td className="px-6 py-4">
                          <span className={`px-2.5 py-0.5 text-xs font-bold rounded-full ${
                            late.customerTier.toUpperCase().includes("1")
                              ? "bg-purple-100 text-purple-800"
                              : "bg-slate-100 text-slate-700"
                          }`}>
                            {late.customerTier}
                          </span>
                        </td>
                        <td className="px-6 py-4 text-xs text-slate-600">
                          {new Date(late.dueDate).toLocaleString([], { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' })}
                        </td>
                        <td className="px-6 py-4 text-xs font-medium text-red-700">
                          {new Date(late.completionDate).toLocaleString([], { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' })}
                        </td>
                        <td className="px-6 py-4 font-semibold text-red-600">
                          +{late.delayHours} hrs
                        </td>
                        <td className="px-6 py-4 text-xs text-slate-600">
                          ₹{late.penaltyRatePerHour}/hr
                        </td>
                        <td className="px-6 py-4 font-bold text-red-700">
                          ₹{late.penaltyAmount.toLocaleString()}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}

          {/* Operator Overtime Breakdown Table */}
          {replanResult.afterCostSummary.operatorOvertimes && replanResult.afterCostSummary.operatorOvertimes.length > 0 && (
            <div className="bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden">
              <div className="px-6 py-4 border-b border-slate-200 bg-amber-50/50 flex justify-between items-center">
                <div>
                  <h3 className="text-base font-bold text-slate-900">
                    ⏱️ Operator Overtime Utilization
                  </h3>
                  <p className="text-xs text-slate-500">
                    Scheduled labor hours exceeding regular shift capacity (8h / 480 mins)
                  </p>
                </div>
                <span className="px-3 py-1 bg-amber-100 text-amber-800 text-xs font-semibold rounded-full">
                  {replanResult.afterCostSummary.operatorOvertimes.length} Shift OTs
                </span>
              </div>

              <div className="overflow-x-auto">
                <table className="w-full text-left text-sm">
                  <thead className="bg-slate-50 border-b border-slate-200 text-slate-600 font-semibold">
                    <tr>
                      <th className="px-6 py-3.5">Operator</th>
                      <th className="px-6 py-3.5">Work Date</th>
                      <th className="px-6 py-3.5">Scheduled Work</th>
                      <th className="px-6 py-3.5">Regular Shift</th>
                      <th className="px-6 py-3.5">Overtime</th>
                      <th className="px-6 py-3.5">Overtime Cost</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100">
                    {replanResult.afterCostSummary.operatorOvertimes.map((ot, idx) => (
                      <tr key={idx} className="hover:bg-slate-50">
                        <td className="px-6 py-4 font-semibold text-slate-900">
                          {ot.operatorName} ({ot.operatorCode})
                        </td>
                        <td className="px-6 py-4 text-slate-600 text-xs">
                          {ot.workDate}
                        </td>
                        <td className="px-6 py-4 text-slate-700">
                          {ot.scheduledMinutes} min ({Math.round(ot.scheduledMinutes / 60.0 * 10) / 10} hrs)
                        </td>
                        <td className="px-6 py-4 text-slate-500 text-xs">
                          {ot.regularMinutes} min (8.0 hrs)
                        </td>
                        <td className="px-6 py-4 font-semibold text-amber-600">
                          +{ot.overtimeMinutes} min ({Math.round(ot.overtimeMinutes / 60.0 * 10) / 10} hrs)
                        </td>
                        <td className="px-6 py-4 font-bold text-amber-700">
                          ₹{ot.overtimeCost.toLocaleString()}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}
        </div>
      )}

      {/* Replan Comparison View */}
      {replanResult && (
        <div className="bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden">
          <div className="px-6 py-4 border-b border-slate-200 bg-slate-50 flex justify-between items-center">
            <div>
              <h2 className="text-lg font-bold text-slate-900">
                Replanning Schedule Impact (Before vs After)
              </h2>
              <p className="text-xs text-slate-500">
                Replanned at {new Date(replanResult.replanTimestamp).toLocaleString()}
              </p>
            </div>
            <span className="px-3 py-1 bg-amber-100 text-amber-800 text-xs font-semibold rounded-full">
              {replanResult.impactDeltas.length} Operations Affected
            </span>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead className="bg-slate-50 border-b border-slate-200 text-slate-600 font-semibold">
                <tr>
                  <th className="px-6 py-3.5">Order</th>
                  <th className="px-6 py-3.5">Operation</th>
                  <th className="px-6 py-3.5">Machine (Before → After)</th>
                  <th className="px-6 py-3.5">Operator (Before → After)</th>
                  <th className="px-6 py-3.5">Time Window (Before → After)</th>
                  <th className="px-6 py-3.5">Delay</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {replanResult.impactDeltas.map((delta, idx) => (
                  <tr key={idx} className="hover:bg-slate-50">
                    <td className="px-6 py-4 font-semibold text-slate-900">
                      {delta.orderNumber}
                    </td>
                    <td className="px-6 py-4 text-slate-600">
                      #{delta.sequenceNumber} {delta.operationType}
                    </td>
                    <td className="px-6 py-4">
                      {delta.machineChanged ? (
                        <div className="flex items-center gap-1.5 font-medium">
                          <span className="text-slate-400 line-through">{delta.beforeMachineCode}</span>
                          <span className="text-slate-400">→</span>
                          <span className="px-2 py-0.5 rounded bg-blue-50 text-blue-700 font-semibold">
                            {delta.afterMachineCode}
                          </span>
                        </div>
                      ) : (
                        <span className="text-slate-700">{delta.afterMachineCode}</span>
                      )}
                    </td>
                    <td className="px-6 py-4 text-slate-600">
                      {delta.operatorChanged ? (
                        <div className="flex items-center gap-1.5 font-medium">
                          <span className="text-slate-400 line-through">{delta.beforeOperatorName}</span>
                          <span className="text-slate-400">→</span>
                          <span className="px-2 py-0.5 rounded bg-indigo-50 text-indigo-700 font-semibold">
                            {delta.afterOperatorName}
                          </span>
                        </div>
                      ) : (
                        <span>{delta.afterOperatorName}</span>
                      )}
                    </td>
                    <td className="px-6 py-4 text-xs text-slate-600">
                      {delta.timeChanged ? (
                        <div className="space-y-0.5">
                          <p className="text-slate-400 line-through">
                            {formatTimeSlot(delta.beforeStartTime, delta.beforeEndTime)}
                          </p>
                          <p className="text-slate-900 font-medium">
                            {formatTimeSlot(delta.afterStartTime, delta.afterEndTime)}
                          </p>
                        </div>
                      ) : (
                        <span>
                          {formatTimeSlot(delta.afterStartTime, delta.afterEndTime)}
                        </span>
                      )}
                    </td>
                    <td className="px-6 py-4">
                      {delta.delayMinutes > 0 ? (
                        <span className="px-2.5 py-1 rounded-full text-xs font-semibold bg-red-100 text-red-700">
                          +{delta.delayMinutes} min
                        </span>
                      ) : (
                        <span className="px-2.5 py-1 rounded-full text-xs font-semibold bg-green-100 text-green-700">
                          On-Time
                        </span>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {replanResult.impactDeltas.length === 0 && (
            <div className="p-8 text-center text-slate-500">
              No schedule adjustments needed. Production schedule is unaffected.
            </div>
          )}
        </div>
      )}

      {/* Breakdowns Table */}
      <div className="bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden">
        <div className="px-6 py-4 border-b border-slate-200 bg-slate-50">
          <h2 className="text-base font-semibold text-slate-900">Machine Breakdown History</h2>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm">
            <thead className="bg-slate-50 border-b border-slate-200 text-slate-600 font-semibold">
              <tr>
                <th className="px-6 py-4">ID</th>
                <th className="px-6 py-4">Machine</th>
                <th className="px-6 py-4">Start Time</th>
                <th className="px-6 py-4">End Time</th>
                <th className="px-6 py-4">Reason</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {breakdowns.map((breakdown) => (
                <tr key={breakdown.id} className="hover:bg-slate-50">
                  <td className="px-6 py-4 font-semibold text-slate-900">#{breakdown.id}</td>
                  <td className="px-6 py-4 text-slate-800 font-medium">
                    {breakdown.machine?.machineCode || "-"}
                  </td>
                  <td className="px-6 py-4 text-slate-600">
                    {breakdown.startTime ? new Date(breakdown.startTime).toLocaleString() : "-"}
                  </td>
                  <td className="px-6 py-4 text-slate-600">
                    {breakdown.endTime ? new Date(breakdown.endTime).toLocaleString() : "-"}
                  </td>
                  <td className="px-6 py-4 text-slate-700">{breakdown.reason || "-"}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        {breakdowns.length === 0 && (
          <div className="p-8 text-center text-slate-500">No breakdowns found.</div>
        )}
      </div>

      {/* Report Breakdown Modal */}
      {showModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 p-4">
          <div className="bg-white rounded-xl shadow-xl border border-slate-200 max-w-lg w-full p-6">
            <h3 className="text-xl font-bold text-slate-900 mb-4">Report Machine Breakdown</h3>

            <form onSubmit={handleSubmitBreakdown} className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1">
                  Select Machine *
                </label>
                <select
                  name="machineId"
                  value={formData.machineId}
                  onChange={handleInputChange}
                  className="w-full px-3.5 py-2.5 border border-slate-300 rounded-lg text-sm outline-none focus:ring-2 focus:ring-slate-400"
                >
                  {machines.map((m) => (
                    <option key={m.id} value={m.id}>
                      {m.machineCode} — {m.name} ({m.type})
                    </option>
                  ))}
                </select>
                {formErrors.machineId && (
                  <p className="text-xs text-red-600 mt-1">{formErrors.machineId}</p>
                )}
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-slate-700 mb-1">
                    Start Time *
                  </label>
                  <input
                    type="datetime-local"
                    name="startTime"
                    value={formData.startTime}
                    onChange={handleInputChange}
                    className="w-full px-3.5 py-2.5 border border-slate-300 rounded-lg text-sm outline-none focus:ring-2 focus:ring-slate-400"
                  />
                  {formErrors.startTime && (
                    <p className="text-xs text-red-600 mt-1">{formErrors.startTime}</p>
                  )}
                </div>

                <div>
                  <label className="block text-sm font-medium text-slate-700 mb-1">
                    End Time *
                  </label>
                  <input
                    type="datetime-local"
                    name="endTime"
                    value={formData.endTime}
                    onChange={handleInputChange}
                    className="w-full px-3.5 py-2.5 border border-slate-300 rounded-lg text-sm outline-none focus:ring-2 focus:ring-slate-400"
                  />
                  {formErrors.endTime && (
                    <p className="text-xs text-red-600 mt-1">{formErrors.endTime}</p>
                  )}
                </div>
              </div>

              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1">
                  Downtime Reason *
                </label>
                <textarea
                  name="reason"
                  rows="3"
                  placeholder="e.g. Spindle bearing overheating, tool wear repair..."
                  value={formData.reason}
                  onChange={handleInputChange}
                  className="w-full px-3.5 py-2.5 border border-slate-300 rounded-lg text-sm outline-none focus:ring-2 focus:ring-slate-400"
                ></textarea>
                {formErrors.reason && (
                  <p className="text-xs text-red-600 mt-1">{formErrors.reason}</p>
                )}
              </div>

              <div className="flex justify-end gap-3 pt-2">
                <button
                  type="button"
                  onClick={() => setShowModal(false)}
                  className="px-4 py-2 text-sm font-medium text-slate-600 hover:bg-slate-100 rounded-lg transition"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={loading}
                  className="px-4 py-2 text-sm font-medium bg-red-600 text-white rounded-lg hover:bg-red-700 transition disabled:opacity-50"
                >
                  {loading ? "Submitting..." : "Submit Breakdown"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}

export default Disruptions;
import { useEffect, useState } from "react";
import api from "../services/api";

function Disruptions() {
  const [breakdowns, setBreakdowns] = useState([]);
  const [absences, setAbsences] = useState([]);
  const [materialDelays, setMaterialDelays] = useState([]);
  const [machines, setMachines] = useState([]);
  const [operators, setOperators] = useState([]);
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(false);
  const [replanLoading, setReplanLoading] = useState(false);
  const [showModal, setShowModal] = useState(false);
  const [showAbsenceModal, setShowAbsenceModal] = useState(false);
  const [showMaterialDelayModal, setShowMaterialDelayModal] = useState(false);
  const [replanResult, setReplanResult] = useState(null);
  const [errorMessage, setErrorMessage] = useState("");
  const [formErrors, setFormErrors] = useState({});
  const [strategy, setStrategy] = useState("MOST_ON_TIME");

  // Breakdown Form state
  const [formData, setFormData] = useState({
    machineId: "",
    startTime: "",
    endTime: "",
    reason: "",
  });

  // Absence Form state
  const [absenceFormData, setAbsenceFormData] = useState({
    operatorId: "",
    startTime: "",
    endTime: "",
    reason: "",
  });

  // Material Delay Form state
  const [materialDelayFormData, setMaterialDelayFormData] = useState({
    orderId: "",
    delayedUntil: "",
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

  const fetchAbsences = () => {
    api.get("/api/operator-absences")
      .then((response) => {
        setAbsences(response.data);
      })
      .catch((error) => {
        console.error("Failed to fetch operator absences:", error);
      });
  };

  const fetchMaterialDelays = () => {
    api.get("/api/material-delays")
      .then((response) => {
        setMaterialDelays(response.data);
      })
      .catch((error) => {
        console.error("Failed to fetch material delays:", error);
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

  const fetchOperators = () => {
    api.get("/api/operators")
      .then((response) => {
        setOperators(response.data);
      })
      .catch((error) => {
        console.error("Failed to fetch operators:", error);
      });
  };

  const fetchOrders = () => {
    api.get("/api/orders")
      .then((response) => {
        setOrders(response.data);
      })
      .catch((error) => {
        console.error("Failed to fetch orders:", error);
      });
  };

  useEffect(() => {
    fetchBreakdowns();
    fetchAbsences();
    fetchMaterialDelays();
    fetchMachines();
    fetchOperators();
    fetchOrders();
    handleTriggerReplan();
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

  const handleOpenAbsenceModal = () => {
    const now = new Date();
    const future = new Date(now.getTime() + 8 * 60 * 60 * 1000);
    const formatLocal = (d) => d.toISOString().slice(0, 16);

    setAbsenceFormData({
      operatorId: operators.length > 0 ? operators[0].id : "",
      startTime: formatLocal(now),
      endTime: formatLocal(future),
      reason: "",
    });
    setFormErrors({});
    setErrorMessage("");
    setShowAbsenceModal(true);
  };

  const handleOpenMaterialDelayModal = () => {
    const now = new Date();
    const future = new Date(now.getTime() + 24 * 60 * 60 * 1000);
    const formatLocal = (d) => d.toISOString().slice(0, 16);

    setMaterialDelayFormData({
      orderId: orders.length > 0 ? orders[0].id : "",
      delayedUntil: formatLocal(future),
      reason: "",
    });
    setFormErrors({});
    setErrorMessage("");
    setShowMaterialDelayModal(true);
  };

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  const handleAbsenceInputChange = (e) => {
    const { name, value } = e.target;
    setAbsenceFormData((prev) => ({ ...prev, [name]: value }));
  };

  const handleMaterialDelayInputChange = (e) => {
    const { name, value } = e.target;
    setMaterialDelayFormData((prev) => ({ ...prev, [name]: value }));
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
        handleTriggerReplan();
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

  const handleSubmitAbsence = (e) => {
    e.preventDefault();
    setErrorMessage("");
    setFormErrors({});

    if (!absenceFormData.operatorId) {
      setErrorMessage("Please select an operator.");
      return;
    }
    if (!absenceFormData.startTime || !absenceFormData.endTime) {
      setErrorMessage("Start time and end time are required.");
      return;
    }
    if (new Date(absenceFormData.endTime) <= new Date(absenceFormData.startTime)) {
      setErrorMessage("End time must be after start time.");
      return;
    }
    if (!absenceFormData.reason.trim()) {
      setErrorMessage("Reason is required.");
      return;
    }

    setLoading(true);
    api.post("/api/operator-absences", {
      operatorId: Number(absenceFormData.operatorId),
      startTime: absenceFormData.startTime,
      endTime: absenceFormData.endTime,
      reason: absenceFormData.reason.trim(),
    })
      .then(() => {
        setShowAbsenceModal(false);
        fetchAbsences();
        handleTriggerReplan();
      })
      .catch((err) => {
        const data = err.response?.data;
        if (data?.fieldErrors) {
          setFormErrors(data.fieldErrors);
        }
        setErrorMessage(data?.message || "Failed to log operator absence.");
      })
      .finally(() => {
        setLoading(false);
      });
  };

  const handleSubmitMaterialDelay = (e) => {
    e.preventDefault();
    setErrorMessage("");
    setFormErrors({});

    if (!materialDelayFormData.orderId) {
      setErrorMessage("Please select an order.");
      return;
    }
    if (!materialDelayFormData.delayedUntil) {
      setErrorMessage("Delayed Until date/time is required.");
      return;
    }
    if (!materialDelayFormData.reason.trim()) {
      setErrorMessage("Reason is required.");
      return;
    }

    setLoading(true);
    api.post("/api/material-delays", {
      orderId: Number(materialDelayFormData.orderId),
      delayedUntil: materialDelayFormData.delayedUntil,
      reason: materialDelayFormData.reason.trim(),
    })
      .then(() => {
        setShowMaterialDelayModal(false);
        fetchMaterialDelays();
        handleTriggerReplan();
      })
      .catch((err) => {
        const data = err.response?.data;
        if (data?.fieldErrors) {
          setFormErrors(data.fieldErrors);
        }
        setErrorMessage(data?.message || "Failed to log material delay.");
      })
      .finally(() => {
        setLoading(false);
      });
  };

  const handleTriggerReplan = (selectedStrategy) => {
    const activeStrategy = selectedStrategy || strategy;
    setReplanLoading(true);
    setErrorMessage("");
    api.post("/api/scheduler/replan", { strategy: activeStrategy })
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
      return `${dateStr} ${startTimeFormatted} - ${endTimeFormatted}`;
    }
    const endDateStr = end.toLocaleDateString([], { month: 'short', day: 'numeric' });
    return `${dateStr} ${startTimeFormatted} - ${endDateStr} ${endTimeFormatted}`;
  };

  const formatDateTime = (dateStr) => {
    if (!dateStr) return "-";
    const date = new Date(dateStr);
    return date.toLocaleString([], {
      month: "short",
      day: "numeric",
      hour: "2-digit",
      minute: "2-digit",
    });
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

        <div className="flex flex-wrap items-center gap-3">
          <div className="flex bg-slate-100 p-1 rounded-lg border border-slate-200">
            {[
              { id: "MOST_ON_TIME", label: "Most On-Time" },
              { id: "CHEAPEST_PRODUCTION", label: "Cheapest" },
              { id: "MOST_ROBUST", label: "Most Robust" },
            ].map((strat) => (
              <button
                key={strat.id}
                onClick={() => {
                  setStrategy(strat.id);
                  handleTriggerReplan(strat.id);
                }}
                className={`px-3 py-1.5 text-xs font-semibold rounded-md transition ${
                  strategy === strat.id
                    ? "bg-white text-slate-900 shadow-sm"
                    : "text-slate-600 hover:text-slate-900"
                }`}
              >
                {strat.label}
              </button>
            ))}
          </div>

          <button
            onClick={handleOpenModal}
            className="px-4 py-2 bg-red-600 text-white text-sm font-medium rounded-lg hover:bg-red-700 transition shadow-sm"
          >
            + Report Breakdown
          </button>
          <button
            onClick={handleOpenAbsenceModal}
            className="px-4 py-2 bg-amber-600 text-white text-sm font-medium rounded-lg hover:bg-amber-700 transition shadow-sm"
          >
            + Report Operator Absence
          </button>
          <button
            onClick={handleOpenMaterialDelayModal}
            className="px-4 py-2 bg-purple-600 text-white text-sm font-medium rounded-lg hover:bg-purple-700 transition shadow-sm"
          >
            + Report Material Delay
          </button>
          <button
            onClick={() => handleTriggerReplan()}
            disabled={replanLoading}
            className="px-4 py-2 bg-slate-900 text-white text-sm font-medium rounded-lg hover:bg-slate-800 transition disabled:opacity-50 shadow-sm"
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
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-7 gap-4">
        <div className="bg-white rounded-xl p-5 shadow-sm border border-slate-200">
          <p className="text-xs font-semibold uppercase tracking-wider text-slate-500">Breakdowns</p>
          <h2 className="text-3xl font-bold text-slate-900 mt-2">{breakdowns.length}</h2>
          <p className="text-xs text-slate-400 mt-1">Machine downtime</p>
        </div>

        <div className="bg-white rounded-xl p-5 shadow-sm border border-slate-200">
          <p className="text-xs font-semibold uppercase tracking-wider text-slate-500">Absences</p>
          <h2 className="text-3xl font-bold text-amber-600 mt-2">{absences.length}</h2>
          <p className="text-xs text-slate-400 mt-1">Operator downtime</p>
        </div>

        <div className="bg-white rounded-xl p-5 shadow-sm border border-slate-200">
          <p className="text-xs font-semibold uppercase tracking-wider text-slate-500">Material Delays</p>
          <h2 className="text-3xl font-bold text-purple-600 mt-2">{materialDelays.length}</h2>
          <p className="text-xs text-slate-400 mt-1">Delayed shipments</p>
        </div>

        {replanResult && (
          <>
            <div className="bg-white rounded-xl p-5 shadow-sm border border-slate-200">
              <p className="text-xs font-semibold uppercase tracking-wider text-slate-500">Ops Shifted</p>
              <h2 className="text-3xl font-bold text-amber-600 mt-2">
                {replanResult.operationsMovedCount}
              </h2>
              <p className="text-xs text-slate-400 mt-1">
                Out of {replanResult.totalOperations} ops
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

          {/* Overtime vs Late Penalty Supervisor Comparison Card */}
          {replanResult && replanResult.overtimeComparison && (
            <div className="bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden">
              <div className="px-6 py-4 border-b border-slate-200 bg-slate-50/80 flex flex-col sm:flex-row justify-between items-start sm:items-center gap-3">
                <div className="flex items-center gap-2.5">
                  <span className="text-xl">⚖️</span>
                  <div>
                    <h3 className="text-base font-bold text-slate-900">
                      Overtime vs Late Penalty Analysis
                    </h3>
                    <p className="text-xs text-slate-500">
                      Economic trade-off comparison between regular-shift recovery and overtime recovery
                    </p>
                  </div>
                </div>

                <div>
                  {replanResult.overtimeComparison.decisionStatus === "OVERTIME_SELECTED" && (
                    <span className="px-3 py-1 bg-green-100 text-green-800 text-xs font-bold rounded-full border border-green-200">
                      ✓ Overtime selected
                    </span>
                  )}
                  {replanResult.overtimeComparison.decisionStatus === "OVERTIME_REJECTED" && (
                    <span className="px-3 py-1 bg-amber-100 text-amber-800 text-xs font-bold rounded-full border border-amber-200">
                      Overtime evaluated but rejected
                    </span>
                  )}
                  {(!replanResult.overtimeComparison.decisionStatus ||
                    replanResult.overtimeComparison.decisionStatus === "NO_DECISION_REQUIRED") && (
                    <span className="px-3 py-1 bg-slate-100 text-slate-700 text-xs font-semibold rounded-full border border-slate-200">
                      No overtime decision required
                    </span>
                  )}
                </div>
              </div>

              <div className="p-6 space-y-5">
                {replanResult.overtimeComparison.decisionStatus === "NO_DECISION_REQUIRED" ||
                !replanResult.overtimeComparison.orderNumber ? (
                  <div className="flex items-center gap-3 p-4 bg-slate-50 rounded-lg text-sm text-slate-600 border border-slate-100">
                    <span className="text-slate-400 text-lg">ℹ️</span>
                    <div>
                      <p className="font-semibold text-slate-800">
                        {replanResult.overtimeComparison.recommendation || "No overtime decision required."}
                      </p>
                      <p className="text-xs text-slate-500 mt-0.5">
                        {replanResult.overtimeComparison.details ||
                          "All operations were recovered using regular shift capacity with zero or minimal delay."}
                      </p>
                    </div>
                  </div>
                ) : (
                  <>
                    {/* Context Header for the affected operation */}
                    <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 p-4 bg-slate-50 rounded-lg border border-slate-100 text-xs">
                      <div>
                        <span className="text-slate-500 block">Affected Order & Op:</span>
                        <span className="font-bold text-slate-900 text-sm">
                          {replanResult.overtimeComparison.orderNumber} #{replanResult.overtimeComparison.sequenceNumber} {replanResult.overtimeComparison.operationType}
                        </span>
                      </div>
                      <div>
                        <span className="text-slate-500 block">Customer / Tier:</span>
                        <span className="font-semibold text-slate-800">
                          {replanResult.overtimeComparison.customerName || "-"} ({replanResult.overtimeComparison.customerTier || "-"})
                        </span>
                      </div>
                      <div>
                        <span className="text-slate-500 block">Delivery Due Date:</span>
                        <span className="font-medium text-slate-700">
                          {formatDateTime(replanResult.overtimeComparison.dueDate)}
                        </span>
                      </div>
                      <div>
                        <span className="text-slate-500 block">Estimated Savings:</span>
                        <span className={`font-bold text-sm ${replanResult.overtimeComparison.savings > 0 ? "text-green-700" : "text-slate-700"}`}>
                          {replanResult.overtimeComparison.savings > 0 ? `₹${replanResult.overtimeComparison.savings.toLocaleString()}` : "₹0"}
                        </span>
                      </div>
                    </div>

                    {/* Comparison Table */}
                    <div className="overflow-x-auto">
                      <table className="w-full text-left text-sm">
                        <thead className="bg-slate-50 border-b border-slate-200 text-slate-600 font-semibold text-xs">
                          <tr>
                            <th className="px-5 py-3">Recovery Option</th>
                            <th className="px-5 py-3">Cost Breakdown</th>
                            <th className="px-5 py-3">Projected Cost</th>
                            <th className="px-5 py-3 text-right">Status</th>
                          </tr>
                        </thead>
                        <tbody className="divide-y divide-slate-100">
                          {/* Regular Shift Recovery */}
                          <tr className={replanResult.overtimeComparison.decisionStatus !== "OVERTIME_SELECTED" ? "bg-green-50/40" : "hover:bg-slate-50"}>
                            <td className="px-5 py-4 font-semibold text-slate-900">
                              Regular recovery
                            </td>
                            <td className="px-5 py-4 text-xs text-slate-600">
                              Projected Late Penalty: ₹{replanResult.overtimeComparison.regularLatePenalty.toLocaleString()}
                            </td>
                            <td className="px-5 py-4 font-bold text-slate-900">
                              ₹{replanResult.overtimeComparison.regularRecoveryCost.toLocaleString()}
                            </td>
                            <td className="px-5 py-4 text-right">
                              {replanResult.overtimeComparison.decisionStatus === "OVERTIME_SELECTED" ? (
                                <span className="px-2.5 py-1 text-xs font-medium rounded-md bg-red-50 text-red-700 border border-red-200">
                                  Higher cost
                                </span>
                              ) : (
                                <span className="px-2.5 py-1 text-xs font-bold rounded-md bg-green-100 text-green-800 border border-green-200">
                                  CHOSEN
                                </span>
                              )}
                            </td>
                          </tr>

                          {/* Overtime Recovery */}
                          <tr className={replanResult.overtimeComparison.decisionStatus === "OVERTIME_SELECTED" ? "bg-green-50/40" : "hover:bg-slate-50"}>
                            <td className="px-5 py-4 font-semibold text-slate-900">
                              Overtime recovery
                            </td>
                            <td className="px-5 py-4 text-xs text-slate-600">
                              Direct Overtime Labor (₹500/hr): ₹{replanResult.overtimeComparison.overtimeLaborCost.toLocaleString()}
                              {replanResult.overtimeComparison.overtimeLatePenalty > 0 && ` + Late Penalty: ₹${replanResult.overtimeComparison.overtimeLatePenalty.toLocaleString()}`}
                            </td>
                            <td className="px-5 py-4 font-bold text-slate-900">
                              ₹{replanResult.overtimeComparison.overtimeRecoveryCost.toLocaleString()}
                            </td>
                            <td className="px-5 py-4 text-right">
                              {replanResult.overtimeComparison.decisionStatus === "OVERTIME_SELECTED" ? (
                                <span className="px-2.5 py-1 text-xs font-bold rounded-md bg-green-100 text-green-800 border border-green-200">
                                  CHOSEN
                                </span>
                              ) : (
                                <span className="px-2.5 py-1 text-xs font-medium rounded-md bg-slate-100 text-slate-600 border border-slate-200">
                                  More expensive
                                </span>
                              )}
                            </td>
                          </tr>

                          {/* Savings Row (if overtime chosen) */}
                          {replanResult.overtimeComparison.decisionStatus === "OVERTIME_SELECTED" && (
                            <tr className="bg-emerald-50/60 font-semibold">
                              <td className="px-5 py-3 text-emerald-900">
                                Net Savings
                              </td>
                              <td className="px-5 py-3 text-xs text-emerald-700">
                                Alternative regular cost − Selected overtime cost
                              </td>
                              <td className="px-5 py-3 font-bold text-emerald-700">
                                ₹{replanResult.overtimeComparison.savings.toLocaleString()}
                              </td>
                              <td className="px-5 py-3 text-right text-xs text-emerald-800 font-bold">
                                SAVED
                              </td>
                            </tr>
                          )}
                        </tbody>
                      </table>
                    </div>

                    {/* Recommendation Box */}
                    <div className={`p-4 rounded-xl border flex items-start gap-3 ${
                      replanResult.overtimeComparison.decisionStatus === "OVERTIME_SELECTED"
                        ? "bg-green-50 border-green-200 text-green-900"
                        : "bg-blue-50 border-blue-200 text-blue-900"
                    }`}>
                      <span className="text-lg">💡</span>
                      <div className="text-sm">
                        <p className="font-bold">
                          Recommendation: "{replanResult.overtimeComparison.recommendation}"
                        </p>
                        <p className="text-xs mt-1 opacity-90">
                          {replanResult.overtimeComparison.details}
                        </p>
                      </div>
                    </div>
                  </>
                )}
              </div>
            </div>
          )}

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
              {breakdowns.length > 0
                ? "No scheduled operations overlapped the logged breakdown windows. Production schedule is unaffected."
                : "No active machine breakdowns logged. Production schedule is operating normally."}
            </div>
          )}
        </div>
      )}

      {/* Breakdowns, Absences & Material Delays Tables */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Breakdowns Table */}
        <div className="bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden">
          <div className="px-6 py-4 border-b border-slate-200 bg-slate-50 flex justify-between items-center">
            <h2 className="text-base font-semibold text-slate-900">Machine Breakdown History</h2>
            <span className="text-xs text-slate-500 font-medium">{breakdowns.length} events</span>
          </div>

          <div className="overflow-x-auto max-h-[300px] overflow-y-auto">
            <table className="w-full text-left text-sm">
              <thead className="bg-slate-50 border-b border-slate-200 text-slate-600 font-semibold sticky top-0">
                <tr>
                  <th className="px-4 py-3">ID</th>
                  <th className="px-4 py-3">Machine</th>
                  <th className="px-4 py-3">Time Window</th>
                  <th className="px-4 py-3">Reason</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {breakdowns.map((breakdown) => (
                  <tr key={breakdown.id} className="hover:bg-slate-50">
                    <td className="px-4 py-3 font-semibold text-slate-900">#{breakdown.id}</td>
                    <td className="px-4 py-3 text-slate-800 font-medium">
                      {breakdown.machine?.machineCode || "-"}
                    </td>
                    <td className="px-4 py-3 text-slate-600 text-xs">
                      {formatTimeSlot(breakdown.startTime, breakdown.endTime)}
                    </td>
                    <td className="px-4 py-3 text-slate-700 text-xs">{breakdown.reason || "-"}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {breakdowns.length === 0 && (
            <div className="p-8 text-center text-slate-500 text-sm">No breakdowns found.</div>
          )}
        </div>

        {/* Operator Absences Table */}
        <div className="bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden">
          <div className="px-6 py-4 border-b border-slate-200 bg-slate-50 flex justify-between items-center">
            <h2 className="text-base font-semibold text-slate-900">Operator Absence History</h2>
            <span className="text-xs text-slate-500 font-medium">{absences.length} events</span>
          </div>

          <div className="overflow-x-auto max-h-[300px] overflow-y-auto">
            <table className="w-full text-left text-sm">
              <thead className="bg-slate-50 border-b border-slate-200 text-slate-600 font-semibold sticky top-0">
                <tr>
                  <th className="px-4 py-3">ID</th>
                  <th className="px-4 py-3">Operator</th>
                  <th className="px-4 py-3">Absence Window</th>
                  <th className="px-4 py-3">Reason</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {absences.map((absence) => (
                  <tr key={absence.id} className="hover:bg-slate-50">
                    <td className="px-4 py-3 font-semibold text-slate-900">#{absence.id}</td>
                    <td className="px-4 py-3 text-slate-800 font-medium">
                      {absence.operator?.name || "-"} ({absence.operator?.operatorCode || "-"})
                    </td>
                    <td className="px-4 py-3 text-slate-600 text-xs">
                      {formatTimeSlot(absence.startTime, absence.endTime)}
                    </td>
                    <td className="px-4 py-3 text-slate-700 text-xs">{absence.reason || "-"}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {absences.length === 0 && (
            <div className="p-8 text-center text-slate-500 text-sm">No operator absences logged.</div>
          )}
        </div>

        {/* Material Delays Table */}
        <div className="bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden">
          <div className="px-6 py-4 border-b border-slate-200 bg-slate-50 flex justify-between items-center">
            <h2 className="text-base font-semibold text-slate-900">Material Delay History</h2>
            <span className="text-xs text-slate-500 font-medium">{materialDelays.length} events</span>
          </div>

          <div className="overflow-x-auto max-h-[300px] overflow-y-auto">
            <table className="w-full text-left text-sm">
              <thead className="bg-slate-50 border-b border-slate-200 text-slate-600 font-semibold sticky top-0">
                <tr>
                  <th className="px-4 py-3">ID</th>
                  <th className="px-4 py-3">Order</th>
                  <th className="px-4 py-3">Delayed Until</th>
                  <th className="px-4 py-3">Reason</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {materialDelays.map((delay) => (
                  <tr key={delay.id} className="hover:bg-slate-50">
                    <td className="px-4 py-3 font-semibold text-slate-900">#{delay.id}</td>
                    <td className="px-4 py-3 text-slate-800 font-medium">
                      {delay.order?.orderNumber || "-"} ({delay.order?.partFamily || "-"})
                    </td>
                    <td className="px-4 py-3 text-slate-600 text-xs font-mono">
                      {formatDateTime(delay.delayedUntil)}
                    </td>
                    <td className="px-4 py-3 text-slate-700 text-xs">{delay.reason || "-"}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {materialDelays.length === 0 && (
            <div className="p-8 text-center text-slate-500 text-sm">No material delays logged.</div>
          )}
        </div>
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
                      {m.machineCode} — {m.name} ({m.machineType})
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
                  Reason *
                </label>
                <textarea
                  name="reason"
                  rows="3"
                  placeholder="e.g. Spindle overheating, electrical failure, hydraulic leak..."
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

      {/* Report Operator Absence Modal */}
      {showAbsenceModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 p-4">
          <div className="bg-white rounded-xl shadow-xl border border-slate-200 max-w-lg w-full p-6">
            <h3 className="text-xl font-bold text-slate-900 mb-4">Report Operator Absence</h3>

            <form onSubmit={handleSubmitAbsence} className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1">
                  Select Operator *
                </label>
                <select
                  name="operatorId"
                  value={absenceFormData.operatorId}
                  onChange={handleAbsenceInputChange}
                  className="w-full px-3.5 py-2.5 border border-slate-300 rounded-lg text-sm outline-none focus:ring-2 focus:ring-slate-400"
                >
                  {operators.map((op) => (
                    <option key={op.id} value={op.id}>
                      {op.name} ({op.operatorCode})
                    </option>
                  ))}
                </select>
                {formErrors.operatorId && (
                  <p className="text-xs text-red-600 mt-1">{formErrors.operatorId}</p>
                )}
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-slate-700 mb-1">
                    Absence Start Time *
                  </label>
                  <input
                    type="datetime-local"
                    name="startTime"
                    value={absenceFormData.startTime}
                    onChange={handleAbsenceInputChange}
                    className="w-full px-3.5 py-2.5 border border-slate-300 rounded-lg text-sm outline-none focus:ring-2 focus:ring-slate-400"
                  />
                  {formErrors.startTime && (
                    <p className="text-xs text-red-600 mt-1">{formErrors.startTime}</p>
                  )}
                </div>

                <div>
                  <label className="block text-sm font-medium text-slate-700 mb-1">
                    Absence End Time *
                  </label>
                  <input
                    type="datetime-local"
                    name="endTime"
                    value={absenceFormData.endTime}
                    onChange={handleAbsenceInputChange}
                    className="w-full px-3.5 py-2.5 border border-slate-300 rounded-lg text-sm outline-none focus:ring-2 focus:ring-slate-400"
                  />
                  {formErrors.endTime && (
                    <p className="text-xs text-red-600 mt-1">{formErrors.endTime}</p>
                  )}
                </div>
              </div>

              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1">
                  Absence Reason *
                </label>
                <textarea
                  name="reason"
                  rows="3"
                  placeholder="e.g. Medical leave, emergency personal leave, unnotified absence..."
                  value={absenceFormData.reason}
                  onChange={handleAbsenceInputChange}
                  className="w-full px-3.5 py-2.5 border border-slate-300 rounded-lg text-sm outline-none focus:ring-2 focus:ring-slate-400"
                ></textarea>
                {formErrors.reason && (
                  <p className="text-xs text-red-600 mt-1">{formErrors.reason}</p>
                )}
              </div>

              <div className="flex justify-end gap-3 pt-2">
                <button
                  type="button"
                  onClick={() => setShowAbsenceModal(false)}
                  className="px-4 py-2 text-sm font-medium text-slate-600 hover:bg-slate-100 rounded-lg transition"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={loading}
                  className="px-4 py-2 text-sm font-medium bg-amber-600 text-white rounded-lg hover:bg-amber-700 transition disabled:opacity-50"
                >
                  {loading ? "Submitting..." : "Submit Absence"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Report Material Delay Modal */}
      {showMaterialDelayModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 p-4">
          <div className="bg-white rounded-xl shadow-xl border border-slate-200 max-w-lg w-full p-6">
            <h3 className="text-xl font-bold text-slate-900 mb-4">Report Material Delay</h3>

            <form onSubmit={handleSubmitMaterialDelay} className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1">
                  Select Order *
                </label>
                <select
                  name="orderId"
                  value={materialDelayFormData.orderId}
                  onChange={handleMaterialDelayInputChange}
                  className="w-full px-3.5 py-2.5 border border-slate-300 rounded-lg text-sm outline-none focus:ring-2 focus:ring-slate-400"
                >
                  {orders.map((ord) => (
                    <option key={ord.id} value={ord.id}>
                      {ord.orderNumber} — Part: {ord.partFamily} (Qty: {ord.quantity})
                    </option>
                  ))}
                </select>
                {formErrors.orderId && (
                  <p className="text-xs text-red-600 mt-1">{formErrors.orderId}</p>
                )}
              </div>

              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1">
                  Delayed Until (Material Arrival) *
                </label>
                <input
                  type="datetime-local"
                  name="delayedUntil"
                  value={materialDelayFormData.delayedUntil}
                  onChange={handleMaterialDelayInputChange}
                  className="w-full px-3.5 py-2.5 border border-slate-300 rounded-lg text-sm outline-none focus:ring-2 focus:ring-slate-400"
                />
                {formErrors.delayedUntil && (
                  <p className="text-xs text-red-600 mt-1">{formErrors.delayedUntil}</p>
                )}
              </div>

              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1">
                  Delay Reason *
                </label>
                <textarea
                  name="reason"
                  rows="3"
                  placeholder="e.g. Raw material supplier shipment delayed, customs clearance hold..."
                  value={materialDelayFormData.reason}
                  onChange={handleMaterialDelayInputChange}
                  className="w-full px-3.5 py-2.5 border border-slate-300 rounded-lg text-sm outline-none focus:ring-2 focus:ring-slate-400"
                ></textarea>
                {formErrors.reason && (
                  <p className="text-xs text-red-600 mt-1">{formErrors.reason}</p>
                )}
              </div>

              <div className="flex justify-end gap-3 pt-2">
                <button
                  type="button"
                  onClick={() => setShowMaterialDelayModal(false)}
                  className="px-4 py-2 text-sm font-medium text-slate-600 hover:bg-slate-100 rounded-lg transition"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={loading}
                  className="px-4 py-2 text-sm font-medium bg-purple-600 text-white rounded-lg hover:bg-purple-700 transition disabled:opacity-50"
                >
                  {loading ? "Submitting..." : "Submit Material Delay"}
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
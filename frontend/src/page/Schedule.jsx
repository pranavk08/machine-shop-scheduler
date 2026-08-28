import { useEffect, useState } from "react";
import api from "../services/api";

function Schedule() {
  const [schedule, setSchedule] = useState([]);
  const [strategy, setStrategy] = useState("MOST_ON_TIME");
  const [loading, setLoading] = useState(false);
  const [comparison, setComparison] = useState(null);
  const [comparisonLoading, setComparisonLoading] = useState(false);
  const [showComparison, setShowComparison] = useState(false);

  const fetchSchedule = (selectedStrategy) => {
    setLoading(true);
    const url = selectedStrategy
      ? `/api/scheduler/orders/schedule?strategy=${selectedStrategy}`
      : "/api/scheduler/orders/schedule";

    api.get(url)
      .then((response) => {
        setSchedule(response.data);
      })
      .catch((error) => {
        console.error("Failed to fetch schedule:", error);
      })
      .finally(() => {
        setLoading(false);
      });
  };

  const handleFetchComparison = () => {
    setComparisonLoading(true);
    api.get("/api/scheduler/strategies/compare")
      .then((response) => {
        setComparison(response.data);
        setShowComparison(true);
      })
      .catch((error) => {
        console.error("Failed to fetch strategy comparison:", error);
      })
      .finally(() => {
        setComparisonLoading(false);
      });
  };

  useEffect(() => {
    fetchSchedule(strategy);
  }, [strategy]);

  const strategies = [
    {
      id: "MOST_ON_TIME",
      label: "🎯 Most On-Time",
      desc: "Prioritize Tier-1 OEMs & earliest deadlines",
    },
    {
      id: "CHEAPEST_PRODUCTION",
      label: "💰 Cheapest Production",
      desc: "Batch compatible part families to cut changeovers",
    },
    {
      id: "MOST_ROBUST",
      label: "🛡️ Most Robust",
      desc: "Schedule bottleneck grinder & tight slack early",
    },
  ];

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h1 className="text-3xl font-bold text-slate-900">
            Production Schedule
          </h1>
          <p className="mt-1 text-slate-500">
            Priority-based scheduling strategies & machine assignment plan
          </p>
        </div>

        <button
          onClick={handleFetchComparison}
          disabled={comparisonLoading}
          className="px-4 py-2.5 bg-indigo-600 text-white font-medium rounded-lg hover:bg-indigo-700 transition shadow-sm flex items-center gap-2 text-sm disabled:opacity-50"
        >
          {comparisonLoading ? "Comparing..." : "⚖️ Compare Strategies"}
        </button>
      </div>

      {/* Strategy Switcher Bar */}
      <div className="bg-white rounded-xl p-3 shadow-sm border border-slate-200">
        <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
          {strategies.map((strat) => {
            const active = strategy === strat.id;
            return (
              <button
                key={strat.id}
                onClick={() => setStrategy(strat.id)}
                className={`p-3.5 rounded-lg text-left transition border ${
                  active
                    ? "bg-slate-900 text-white border-slate-900 shadow-sm"
                    : "bg-slate-50 hover:bg-slate-100 text-slate-700 border-slate-200"
                }`}
              >
                <div className="flex items-center justify-between">
                  <span className="font-bold text-sm">{strat.label}</span>
                  {active && (
                    <span className="px-2 py-0.5 rounded text-[10px] uppercase font-bold bg-white/20 text-white">
                      Active
                    </span>
                  )}
                </div>
                <p className={`text-xs mt-1 ${active ? "text-slate-300" : "text-slate-500"}`}>
                  {strat.desc}
                </p>
              </button>
            );
          })}
        </div>
      </div>

      {/* Strategy Comparison Matrix View */}
      {showComparison && comparison && (
        <div className="bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden">
          <div className="px-6 py-4 border-b border-slate-200 bg-slate-50 flex justify-between items-center">
            <div>
              <h2 className="text-lg font-bold text-slate-900">
                ⚖️ Side-by-Side Strategy Evaluation
              </h2>
              <p className="text-xs text-slate-500">
                Evaluated against current open orders dataset
              </p>
            </div>
            <button
              onClick={() => setShowComparison(false)}
              className="text-slate-400 hover:text-slate-600 text-xs font-semibold px-2 py-1"
            >
              ✕ Hide
            </button>
          </div>

          {/* Recommendation Banner */}
          {comparison.recommendationReason && (
            <div className="p-4 bg-emerald-50 border-b border-emerald-100 flex items-start gap-3">
              <span className="text-xl">💡</span>
              <div>
                <p className="text-sm font-semibold text-emerald-900">Recommendation</p>
                <p className="text-xs text-emerald-800 mt-0.5">{comparison.recommendationReason}</p>
              </div>
            </div>
          )}

          <div className="p-6 grid grid-cols-1 md:grid-cols-3 gap-5">
            {comparison.strategies.map((item) => {
              const isRecommended = item.strategy === comparison.recommendedStrategy;
              const isSelected = strategy === item.strategy;
              return (
                <div
                  key={item.strategy}
                  className={`rounded-xl p-5 border relative flex flex-col justify-between ${
                    isRecommended
                      ? "border-emerald-500 bg-emerald-50/20 shadow-md ring-1 ring-emerald-500"
                      : "border-slate-200 bg-white"
                  }`}
                >
                  {isRecommended && (
                    <div className="absolute -top-3 right-4 px-3 py-0.5 rounded-full bg-emerald-600 text-white text-[11px] font-bold shadow-sm">
                      ⭐ Recommended
                    </div>
                  )}

                  <div>
                    <h3 className="font-bold text-slate-900 text-base">{item.displayName}</h3>
                    <p className="text-xs text-slate-500 mt-1 min-h-[32px]">{item.description}</p>

                    {/* Key Metrics Grid */}
                    <div className="grid grid-cols-2 gap-3 mt-4 pt-3 border-t border-slate-100 text-xs">
                      <div className="bg-slate-50 p-2.5 rounded-lg">
                        <p className="text-slate-400">Total Cost</p>
                        <p className="text-base font-bold text-slate-900 mt-0.5">
                          ₹{item.costSummary?.totalCost.toLocaleString() || 0}
                        </p>
                      </div>

                      <div className="bg-slate-50 p-2.5 rounded-lg">
                        <p className="text-slate-400">Late Orders</p>
                        <p className={`text-base font-bold mt-0.5 ${
                          item.costSummary?.lateOrdersCount > 0 ? "text-red-600" : "text-emerald-600"
                        }`}>
                          {item.costSummary?.lateOrdersCount || 0}
                        </p>
                      </div>

                      <div className="bg-slate-50 p-2.5 rounded-lg">
                        <p className="text-slate-400">Overtime Cost</p>
                        <p className="font-bold text-amber-700 mt-0.5">
                          ₹{item.costSummary?.totalOvertimeCost.toLocaleString() || 0}
                        </p>
                      </div>

                      <div className="bg-slate-50 p-2.5 rounded-lg">
                        <p className="text-slate-400">Penalty Exposure</p>
                        <p className="font-bold text-red-600 mt-0.5">
                          ₹{item.costSummary?.totalPenaltyCost.toLocaleString() || 0}
                        </p>
                      </div>
                    </div>

                    <div className="mt-3 flex justify-between text-xs text-slate-500 px-1">
                      <span>Operations: <strong>{item.totalOperations}</strong></span>
                      <span>Span: <strong>{item.totalScheduleDurationHours} hrs</strong></span>
                    </div>
                  </div>

                  <button
                    onClick={() => setStrategy(item.strategy)}
                    className={`mt-4 w-full py-2 text-xs font-semibold rounded-lg transition ${
                      isSelected
                        ? "bg-slate-900 text-white"
                        : "bg-slate-100 hover:bg-slate-200 text-slate-700"
                    }`}
                  >
                    {isSelected ? "✓ Active Strategy" : "Apply This Strategy"}
                  </button>
                </div>
              );
            })}
          </div>
        </div>
      )}

      {/* Schedule Table */}
      <div className="bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden">
        <div className="px-6 py-4 border-b border-slate-200 bg-slate-50 flex justify-between items-center">
          <h2 className="text-base font-semibold text-slate-900">
            Operations Sequence ({schedule.length} Operations)
          </h2>
          {loading && (
            <span className="text-xs text-indigo-600 font-semibold animate-pulse">
              Calculating schedule...
            </span>
          )}
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm">
            <thead className="bg-slate-50 border-b border-slate-200 text-slate-600 font-semibold">
              <tr>
                <th className="px-6 py-3.5">Order</th>
                <th className="px-6 py-3.5">Customer / Part</th>
                <th className="px-6 py-3.5">Operation</th>
                <th className="px-6 py-3.5">Seq</th>
                <th className="px-6 py-3.5">Machine</th>
                <th className="px-6 py-3.5">Operator</th>
                <th className="px-6 py-3.5">Start Time</th>
                <th className="px-6 py-3.5">End Time</th>
              </tr>
            </thead>

            <tbody className="divide-y divide-slate-100">
              {schedule.map((item, index) => (
                <tr key={index} className="hover:bg-slate-50">
                  <td className="px-6 py-3.5 font-semibold text-slate-900">
                    {item.operation?.order?.orderNumber || "-"}
                  </td>
                  <td className="px-6 py-3.5 text-xs">
                    <span className="font-medium text-slate-700">
                      {item.operation?.order?.customer?.name || "Customer"}
                    </span>
                    <span className="ml-1.5 px-2 py-0.5 rounded text-[10px] font-bold bg-slate-100 text-slate-600">
                      {item.operation?.order?.partFamily || "-"}
                    </span>
                  </td>
                  <td className="px-6 py-3.5 text-slate-700">
                    {item.operation?.operationType || "-"}
                  </td>
                  <td className="px-6 py-3.5 text-slate-500 font-mono text-xs">
                    #{item.operation?.sequenceNumber || "-"}
                  </td>
                  <td className="px-6 py-3.5">
                    <span className="px-2 py-0.5 rounded bg-blue-50 text-blue-700 font-semibold text-xs">
                      {item.machine?.machineCode || "-"}
                    </span>
                  </td>
                  <td className="px-6 py-3.5 text-slate-700">
                    {item.operator?.name || "-"}
                  </td>
                  <td className="px-6 py-3.5 text-xs text-slate-600">
                    {item.startTime ? new Date(item.startTime).toLocaleString([], { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' }) : "-"}
                  </td>
                  <td className="px-6 py-3.5 text-xs text-slate-600">
                    {item.endTime ? new Date(item.endTime).toLocaleString([], { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' }) : "-"}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        {schedule.length === 0 && !loading && (
          <div className="p-8 text-center text-slate-500">
            No schedule data found.
          </div>
        )}
      </div>
    </div>
  );
}

export default Schedule;
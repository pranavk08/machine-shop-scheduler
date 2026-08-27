import { useEffect, useState } from "react";
import api from "../services/api";

function Orders() {

  const [orders, setOrders] = useState([]);

  useEffect(() => {

    api.get("/api/orders/open")
      .then((response) => {
        console.log("Orders page data:", response.data);
        setOrders(response.data);
      })
      .catch((error) => {
        console.error("Failed to fetch orders:", error);
      });

  }, []);

  return (
    <div>

      {/* Header */}
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-slate-900">
          Orders
        </h1>

        <p className="mt-2 text-slate-500">
          Open production orders
        </p>
      </div>

      {/* Order Summary */}
      <div className="bg-white rounded-xl p-6 shadow-sm border border-slate-200 mb-6">

        <p className="text-sm text-slate-500">
          Total Open Orders
        </p>

        <h2 className="text-3xl font-bold text-slate-900 mt-2">
          {orders.length}
        </h2>

      </div>

      {/* Orders Table */}
      <div className="bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden">

        <div className="overflow-x-auto">

          <table className="w-full text-left">

            <thead className="bg-slate-50 border-b border-slate-200">
              <tr>
                <th className="px-6 py-4 text-sm font-semibold text-slate-600">
                  Order
                </th>

                <th className="px-6 py-4 text-sm font-semibold text-slate-600">
                  Customer
                </th>

                <th className="px-6 py-4 text-sm font-semibold text-slate-600">
                  Part Family
                </th>

                <th className="px-6 py-4 text-sm font-semibold text-slate-600">
                  Quantity
                </th>

                <th className="px-6 py-4 text-sm font-semibold text-slate-600">
                  Due Date
                </th>

                <th className="px-6 py-4 text-sm font-semibold text-slate-600">
                  Status
                </th>
              </tr>
            </thead>

            <tbody>

              {orders.map((order) => (

                <tr
                  key={order.id}
                  className="border-b border-slate-100 hover:bg-slate-50"
                >

                  <td className="px-6 py-4 font-medium text-slate-900">
                    {order.orderNumber}
                  </td>

                  <td className="px-6 py-4 text-slate-600">
                    {order.customer?.name || "-"}
                  </td>

                  <td className="px-6 py-4 text-slate-600">
                    {order.partFamily}
                  </td>

                  <td className="px-6 py-4 text-slate-600">
                    {order.quantity}
                  </td>

                  <td className="px-6 py-4 text-slate-600">
                    {new Date(order.dueDate).toLocaleString()}
                  </td>

                  <td className="px-6 py-4">

                    <span className="px-3 py-1 rounded-full text-xs font-medium bg-green-100 text-green-700">
                      {order.status}
                    </span>

                  </td>

                </tr>

              ))}

            </tbody>

          </table>

        </div>

        {orders.length === 0 && (
          <div className="p-8 text-center text-slate-500">
            No open orders found.
          </div>
        )}

      </div>

    </div>
  );
}

export default Orders;
import { api } from "../api";

export const createOrder = (data) =>
  api("/orders", { method: "POST", body: JSON.stringify(data) });
export const fetchMyOrders = () => api("/orders");
export const fetchMyOrder = (id) => api("/orders/" + id);
export const cancelMyOrder = (id) =>
  api("/orders/" + id + "/cancel", { method: "PUT" });
export const confirmOrderReceipt = (id) =>
  api("/orders/" + id + "/confirm-receipt", { method: "PUT" });

import { api } from "../api";

export const fetchNotifications = () => api("/notifications");
export const markNotificationRead = (id) =>
  api("/notifications/" + id + "/read", { method: "PUT" });

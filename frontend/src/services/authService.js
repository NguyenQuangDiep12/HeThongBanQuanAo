import { api } from "../api";

export const registerAccount = (data) =>
  api("/auth/register", { method: "POST", body: JSON.stringify(data) });
export const loginAccount = (data) =>
  api("/auth/login", { method: "POST", body: JSON.stringify(data) });

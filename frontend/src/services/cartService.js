import { api } from "../api";

export const fetchCart = () => api("/cart");
export const addCartItem = (data) =>
  api("/cart/items", { method: "POST", body: JSON.stringify(data) });
export const updateCartItem = (id, quantity) =>
  api("/cart/items/" + id, {
    method: "PUT",
    body: JSON.stringify({ quantity }),
  });
export const removeCartItem = (id) =>
  api("/cart/items/" + id, { method: "DELETE" });

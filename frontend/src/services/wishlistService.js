import { api } from "../api";

export const fetchWishlist = () => api("/wishlist");
export const addToWishlist = (productId) =>
  api(`/wishlist/${productId}`, { method: "POST" });
export const removeFromWishlist = (productId) =>
  api(`/wishlist/${productId}`, { method: "DELETE" });

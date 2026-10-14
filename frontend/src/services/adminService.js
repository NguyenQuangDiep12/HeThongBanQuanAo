import { api } from "../api";

export const fetchAdminProducts = () => api("/admin/products");
export const fetchAdminCategories = () => api("/admin/categories");
export const fetchAdminOrders = () => api("/admin/orders");

export const saveAdminCategory = (id, data) =>
  api(id == null ? "/admin/categories" : "/admin/categories/" + id, {
    method: id == null ? "POST" : "PUT",
    body: JSON.stringify(data),
  });

export const deleteAdminCategory = (id) =>
  api("/admin/categories/" + id, { method: "DELETE" });

export const saveAdminProduct = (id, data) =>
  api(id == null ? "/admin/products" : "/admin/products/" + id, {
    method: id == null ? "POST" : "PUT",
    body: JSON.stringify(data),
  });
export const addAdminProductVariant = (productId, data) =>
  api("/admin/products/" + productId + "/variants", {
    method: "POST",
    body: JSON.stringify(data),
  });
export const updateAdminProductVariant = (productId, variantId, data) =>
  api("/admin/products/" + productId + "/variants/" + variantId, {
    method: "PUT",
    body: JSON.stringify(data),
  });

export const deleteAdminProductVariant = (productId, variantId) =>
  api("/admin/products/" + productId + "/variants/" + variantId, {
    method: "DELETE",
  });
export const deactivateAdminProduct = (id) =>
  api("/admin/products/" + id, { method: "DELETE" });
export const updateAdminOrderStatus = (orderId, action) =>
  api("/admin/orders/" + orderId + "/" + action, { method: "PUT" });

import { api } from "../api";

export const fetchFeaturedProducts = () =>
  api("/products?page=0&pageSize=8&sort=newest");
export const fetchProductCategories = () => api("/products/categories");
export const fetchProductFilters = () => api("/products/filters");
export const searchProducts = (params) => api("/products?" + params.toString());
export const fetchProduct = (id) => api("/products/" + id);
export const fetchRelatedProducts = (id, limit = 4) =>
  api("/products/" + id + "/related?limit=" + limit);

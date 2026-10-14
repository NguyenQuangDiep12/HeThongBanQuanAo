import { createContext, useContext, useEffect, useMemo, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { addToWishlist, fetchWishlist, removeFromWishlist } from "../../services/wishlistService.js";

const WishlistContext = createContext(null);

export function WishlistProvider({ user, setToast, children }) {
  const [ids, setIds] = useState(() => new Set());
  const navigate = useNavigate();
  const location = useLocation();
  useEffect(() => {
    let active = true;
    setIds(new Set());
    if (user?.role === "USER") fetchWishlist().then((items) => {
      if (active) setIds(new Set(items.map((item) => String(item.id))));
    }).catch(() => {});
    return () => { active = false; };
  }, [user?.role, user?.id, user?.userId]);
  const toggle = async (productId) => {
    if (!user) {
      navigate("/dang-nhap?redirect=" + encodeURIComponent(location.pathname + location.search));
      return false;
    }
    if (user.role !== "USER") { setToast?.("Danh sách yêu thích dành cho tài khoản khách hàng."); return false; }
    const key = String(productId);
    try {
      if (ids.has(key)) {
        await removeFromWishlist(productId);
        setIds((current) => { const next = new Set(current); next.delete(key); return next; });
        setToast?.("Đã xóa khỏi danh sách yêu thích");
      } else {
        await addToWishlist(productId);
        setIds((current) => new Set(current).add(key));
        setToast?.("Đã lưu vào danh sách yêu thích");
      }
      return true;
    } catch (error) { setToast?.(error.message); return false; }
  };
  const remove = async (productId) => {
    if (!user || user.role !== "USER") return false;
    const key = String(productId);
    try {
      await removeFromWishlist(productId);
      setIds((current) => { const next = new Set(current); next.delete(key); return next; });
      setToast?.("Đã xóa khỏi danh sách yêu thích");
      return true;
    } catch (error) { setToast?.(error.message); return false; }
  };
  const value = useMemo(() => ({ ids, toggle, remove }), [ids, user]);
  return <WishlistContext.Provider value={value}>{children}</WishlistContext.Provider>;
}

export function useWishlist() {
  const value = useContext(WishlistContext);
  if (!value) throw new Error("useWishlist must be used within WishlistProvider");
  return value;
}

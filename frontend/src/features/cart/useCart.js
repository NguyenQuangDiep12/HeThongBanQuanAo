import { useEffect, useState } from "react";
import { getToken } from "../../api";
import { fetchCart } from "../../services/cartService.js";

export function useCart() {
  const [cart, setCart] = useState(null), 
  [error, setError] = useState("");
  const load = () =>
    getToken()
      ? fetchCart()
          .then(setCart)
          .catch((e) => setError(e.message))
      : setCart(null);
  useEffect(() => {
    load();
  }, []);
  return {
    cart,
    setCart,
    error,
    load,
  };
}

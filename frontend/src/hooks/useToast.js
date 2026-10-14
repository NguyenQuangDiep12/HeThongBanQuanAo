import { useEffect, useState } from "react";

export function useToast() {
  const [toast, set] = useState("");
  useEffect(() => {
    if (!toast) return;
    let t = setTimeout(() => set(""), 3000);
    return () => clearTimeout(t);
  }, [toast]);
  return {
    toast,
    setToast: set,
  };
}

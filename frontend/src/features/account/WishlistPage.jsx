import { useEffect, useState } from "react";
import { Heart, Trash2 } from "lucide-react";
import { Link } from "react-router-dom";
import { money } from "../../api";
import { pics } from "../../shared/media.js";
import { Title } from "../../components/ui/PageTitle.jsx";
import { Empty } from "../../components/ui/EmptyState.jsx";
import { fetchWishlist } from "../../services/wishlistService.js";
import { useWishlist } from "./WishlistContext.jsx";

export function Wishlist() {
  const { remove: removeFromWishlist } = useWishlist();
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  useEffect(() => {
    fetchWishlist().then(setItems).catch((e) => setError(e.message)).finally(() => setLoading(false));
  }, []);
  const remove = async (product) => {
    const removed = await removeFromWishlist(product.id);
    if (removed) {
      setItems((current) => current.filter((item) => item.id !== product.id));
    }
  };
  return (
    <main className="shell py-12">
      <Title eyebrow="Bộ sưu tập cá nhân" title="Sản phẩm yêu thích" description="Những món đồ bạn muốn lưu lại để xem sau." />
      {loading ? <p className="py-12 text-center text-sm text-slate-400">Đang tải danh sách…</p>
        : error ? <Empty Icon={Heart} title="Chưa tải được danh sách" body={error} to="/tai-khoan" action="Về tài khoản" />
          : !items.length ? <Empty Icon={Heart} title="Danh sách đang trống" body="Lưu sản phẩm bạn thích để tìm lại nhanh hơn." />
            : <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-4 lg:gap-6">{items.map((product) => (
              <article key={product.id} className="group relative">
                <Link to={`/san-pham/${product.id}`} className="block aspect-[.82] overflow-hidden rounded-2xl bg-slate-100">
                  <img src={product.imageUrl || pics.fallback} onError={(e) => { e.currentTarget.src = pics.fallback; }} alt={product.name} className="h-full w-full object-cover transition duration-500 group-hover:scale-105" />
                </Link>
                <button type="button" onClick={() => remove(product)} aria-label={`Xóa ${product.name} khỏi yêu thích`} className="absolute right-3 top-3 rounded-full bg-white p-2 text-rose-600 shadow-sm hover:bg-rose-50"><Trash2 size={16} /></button>
                <p className="mt-3 text-[10px] font-bold uppercase tracking-widest text-slate-400">{product.category}</p>
                <Link to={`/san-pham/${product.id}`} className="mt-1 block truncate text-sm font-bold hover:text-brand-700">{product.name}</Link>
                <b className="mt-2 block text-sm">{money(product.price)}</b>
              </article>
            ))}</div>}
    </main>
  );
}

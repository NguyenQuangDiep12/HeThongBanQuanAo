import { useEffect, useState } from "react";
import { Link, useLocation, useNavigate, useParams } from "react-router-dom";
import {
  Heart,
  Minus,
  Plus,
  ShieldCheck,
  ShoppingBag,
  Truck,
  CreditCard,
} from "lucide-react";
import { money } from "../../api";
import { pics } from "../../shared/media.js";
import { cx, primary, secondary, label } from "../../shared/uiStyles.js";
import { Title } from "../../components/ui/PageTitle.jsx";
import { Empty } from "../../components/ui/EmptyState.jsx";
import { Grid } from "../../components/products/ProductGrid.jsx";
import {
  fetchProduct,
  fetchRelatedProducts,
} from "../../services/productService.js";
import { addCartItem } from "../../services/cartService.js";

export function Detail({ user, setToast, onCart }) {
  const { id } = useParams(),
    location = useLocation(),
    navigate = useNavigate(),
    [p, setP] = useState(null),
    [related, setRelated] = useState([]),
    [v, setV] = useState(null),
    [q, setQ] = useState(1),
    [error, setError] = useState("");
  useEffect(() => {
    fetchProduct(id)
      .then((x) => {
        setP(x);
        setV(x.variants?.find((y) => y.stockQuantity > 0) || x.variants?.[0]);
      })
      .catch((e) => setError(e.message));
    fetchRelatedProducts(id, 4)
      .then(setRelated)
      .catch(() => {});
  }, [id]);
  if (!p)
    return (
      <main className="shell py-20">
        {error ? (
          <Empty title="Không tìm thấy sản phẩm" body={error} />
        ) : (
          <p className="text-center text-slate-400">Đang tải sản phẩm…</p>
        )}
      </main>
    );
  const variants = p.variants || [],
    sizes = [...new Set(variants.map((x) => x.size))],
    colors = [...new Set(variants.map((x) => x.color))],
    pick = (s, c) =>
      setV(
        variants.find((x) => x.size === s && x.color === c) ||
          variants.find((x) => x.size === s) ||
          variants.find((x) => x.color === c),
      );
  const add = async () => {
    if (!user) {
      navigate(
        "/dang-nhap?redirect=" + encodeURIComponent(location.pathname + location.search),
      );
      return;
    }
    if (user.role !== "USER") {
      setToast("Tài khoản ADMIN không đặt hàng. Hãy đăng nhập bằng tài khoản khách hàng.");
      return;
    }
    try {
      await addCartItem({
        variantId: v.id,
        quantity: q,
      });
      onCart();
      setToast("Đã thêm vào giỏ hàng");
    } catch (e) {
      setToast(e.message);
    }
  };
  return (
    <main className="shell py-10">
      <p className="text-xs text-slate-400">
        <Link to="/">Trang chủ</Link> / <Link to="/san-pham">Sản phẩm</Link> /{" "}
        {p.name}
      </p>
      <div className="mt-6 grid gap-10 lg:grid-cols-2">
        <img
          src={p.imageUrl || pics.fallback}
          onError={(e) => (e.currentTarget.src = pics.fallback)}
          alt={p.name}
          className="aspect-[.9] w-full rounded-[24px] bg-slate-100 object-cover"
        />
        <section className="py-2">
          <p className="text-[10px] font-extrabold uppercase tracking-widest text-brand-700">
            {p.category || "Bộ sưu tập"}
          </p>
          <h1 className="mt-3 font-display text-3xl font-extrabold tracking-tight sm:text-4xl">
            {p.name}
          </h1>
          <div className="mt-4 flex items-center gap-3">
            <b className="font-display text-2xl">
              {money(v?.price || p.price)}
            </b>
            <span className="rounded-full bg-emerald-50 px-3 py-1 text-xs font-bold text-emerald-700">
              {v?.stockQuantity > 0
                ? "Còn " + v.stockQuantity + " sản phẩm"
                : "Tạm hết hàng"}
            </span>
          </div>
          <hr className="my-6 border-line" />
          <p className="whitespace-pre-line text-sm leading-7 text-slate-500">
            {p.description ||
              "Thiết kế tinh giản, chất liệu mềm mại, dễ phối trong nhiều hoàn cảnh."}
          </p>
          <div className="mt-7">
            <p className={label}>Kích cỡ</p>
            <div className="flex gap-2">
              {sizes.map((s) => (
                <button
                  onClick={() => pick(s, v?.color)}
                  key={s}
                  className={cx(
                    "rounded-xl border px-4 py-3 text-xs font-bold",
                    v?.size === s ? "bg-brand-800 text-white" : "border-line",
                  )}
                >
                  {s}
                </button>
              ))}
            </div>
          </div>
          <div className="mt-6">
            <p className={label}>Màu sắc · {v?.color}</p>
            <div className="flex gap-2">
              {colors.map((c) => (
                <button
                  onClick={() => pick(v?.size, c)}
                  key={c}
                  className={cx(
                    "rounded-full border px-4 py-2 text-xs",
                    v?.color === c
                      ? "border-brand-700 bg-brand-50 text-brand-800"
                      : "border-line",
                  )}
                >
                  {c}
                </button>
              ))}
            </div>
          </div>
          <div className="mt-7 flex gap-3">
            <div className="flex items-center rounded-xl border border-line">
              <button
                disabled={q <= 1}
                onClick={() => setQ(q - 1)}
                className="p-3"
              >
                <Minus size={15} />
              </button>
              <b className="w-6 text-center text-sm">{q}</b>
              <button
                disabled={q >= (v?.stockQuantity || 0)}
                onClick={() => setQ(q + 1)}
                className="p-3"
              >
                <Plus size={15} />
              </button>
            </div>
            <button
              disabled={!v || !v.stockQuantity}
              onClick={add}
              className={primary + " flex-1"}
            >
              <ShoppingBag size={16} />
              {user?.role === "USER" ? "Thêm vào giỏ" : "Đăng nhập để mua"}
            </button>
            <button className={secondary}>
              <Heart size={18} />
            </button>
          </div>
          <div className="mt-7 grid gap-3 border-t border-line pt-5 text-xs text-slate-500 sm:grid-cols-2">
            <span>
              <Truck size={15} className="mr-2 inline text-brand-700" /> Giao
              hàng toàn quốc
            </span>
            <span>
              <ShieldCheck size={15} className="mr-2 inline text-brand-700" />{" "}
              Đổi trả trong 7 ngày
            </span>
            <span>
              <CreditCard size={15} className="mr-2 inline text-brand-700" />{" "}
              COD / chuyển khoản
            </span>
          </div>
        </section>
      </div>
      {related.length > 0 && (
        <section className="mt-16">
          <Title eyebrow="Gợi ý cho bạn" title="Sản phẩm liên quan" />
          <Grid items={related} />
        </section>
      )}
    </main>
  );
}

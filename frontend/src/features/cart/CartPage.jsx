import { Link } from "react-router-dom";
import {
  ArrowRight,
  LockKeyhole,
  Minus,
  Plus,
  ShoppingCart,
  X,
} from "lucide-react";
import { getToken, money } from "../../api";
import { pics } from "../../shared/media.js";
import { primary } from "../../shared/uiStyles.js";
import { Title } from "../../components/ui/PageTitle.jsx";
import { Empty } from "../../components/ui/EmptyState.jsx";
import { useCart } from "./useCart.js";
import { updateCartItem, removeCartItem } from "../../services/cartService.js";

export function Cart({ setToast, onChange }) {
  const { cart, setCart, error, load } = useCart();
  if (!getToken())
    return (
      <main className="shell py-14">
        <Empty
          Icon={LockKeyhole}
          title="Đăng nhập để xem giỏ hàng"
          body="Giỏ hàng được lưu theo tài khoản của bạn."
          to="/dang-nhap?redirect=%2Fgio-hang"
          action="Đăng nhập"
        />
      </main>
    );
  const change = async (i, n) => {
    try {
      setCart(await updateCartItem(i.id, n));
      onChange();
    } catch (e) {
      setToast(e.message);
    }
  };
  const remove = async (i) => {
    try {
      await removeCartItem(i.id);
      load();
      onChange();
    } catch (e) {
      setToast(e.message);
    }
  };
  return (
    <main className="shell py-10">
      <Title
        eyebrow="Giỏ hàng của bạn"
        title="Giỏ hàng"
        description="Kiểm tra sản phẩm và số lượng trước khi thanh toán."
      />
      <div className="grid gap-7 lg:grid-cols-[1fr_330px]">
        {error ? (
          <p className="text-sm text-rose-700">{error}</p>
        ) : !cart ? (
          <p>Đang tải giỏ hàng…</p>
        ) : cart.items?.length ? (
          <>
            <section className="divide-y divide-line rounded-2xl border border-line">
              {cart.items.map((i) => (
                <article
                  key={i.id}
                  className="grid grid-cols-[62px_1fr_auto] items-center gap-3 p-4 sm:grid-cols-[1fr_120px_120px_30px] sm:p-5"
                >
                  <div className="flex items-center gap-3">
                    <img
                      src={pics.fallback}
                      className="h-20 w-16 rounded-xl object-cover"
                    />
                    <div>
                      <Link
                        to={"/san-pham/" + i.productId}
                        className="text-sm font-bold"
                      >
                        {i.productName}
                      </Link>
                      <p className="mt-1 text-xs text-slate-500">
                        {i.color} · {i.size}
                      </p>
                      <small className="text-slate-400">
                        Còn {i.availableStock}
                      </small>
                    </div>
                  </div>
                  <b className="hidden text-sm sm:block">
                    {money(i.unitPrice)}
                  </b>
                  <div className="flex items-center gap-2">
                    <button
                      disabled={i.quantity <= 1}
                      onClick={() => change(i, i.quantity - 1)}
                    >
                      <Minus size={14} />
                    </button>
                    <b>{i.quantity}</b>
                    <button
                      disabled={i.quantity >= i.availableStock}
                      onClick={() => change(i, i.quantity + 1)}
                    >
                      <Plus size={14} />
                    </button>
                  </div>
                  <button onClick={() => remove(i)}>
                    <X size={16} />
                  </button>
                </article>
              ))}
            </section>
            <aside className="h-fit rounded-2xl border border-line p-6">
              <h2 className="font-display font-extrabold">Tóm tắt đơn hàng</h2>
              <div className="mt-5 flex justify-between border-b border-line pb-4 text-sm">
                <span>Tạm tính</span>
                <b>{money(cart.totalAmount)}</b>
              </div>
              <div className="flex justify-between py-4 text-sm">
                <span>Vận chuyển</span>
                <span className="text-emerald-700">Xác nhận sau</span>
              </div>
              <div className="flex justify-between border-t border-line pt-4 font-extrabold">
                <span>Tổng cộng</span>
                <b className="text-brand-800">{money(cart.totalAmount)}</b>
              </div>
              <Link to="/thanh-toan" className={primary + " mt-6 w-full"}>
                Tiến hành đặt hàng <ArrowRight size={15} />
              </Link>
              <Link
                to="/san-pham"
                className="mt-4 block text-center text-xs font-bold text-slate-500"
              >
                ← Tiếp tục mua sắm
              </Link>
            </aside>
          </>
        ) : (
          <div className="lg:col-span-2">
            <Empty
              Icon={ShoppingCart}
              title="Giỏ hàng đang trống"
              body="Khám phá bộ sưu tập và chọn món đồ yêu thích."
            />
          </div>
        )}
      </div>
    </main>
  );
}

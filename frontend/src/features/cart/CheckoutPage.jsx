import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { ArrowRight, LockKeyhole } from "lucide-react";
import { getToken, money } from "../../api";
import { primary, input, label } from "../../shared/uiStyles.js";
import { Title } from "../../components/ui/PageTitle.jsx";
import { Empty } from "../../components/ui/EmptyState.jsx";
import { useCart } from "./useCart.js";
import { createOrder } from "../../services/orderService.js";

export function Checkout({ setToast, onChange }) {
  const { cart, error } = useCart(),
    nav = useNavigate(),
    [form, setForm] = useState({
      shippingAddress: "",
      phone: "",
      paymentMethod: "COD",
    }),
    [busy, setBusy] = useState(false);
  if (!getToken())
    return (
      <main className="shell py-14">
        <Empty
          Icon={LockKeyhole}
          title="Đăng nhập để đặt hàng"
          body="Đăng nhập để tiếp tục checkout."
          to="/dang-nhap?redirect=%2Fthanh-toan"
          action="Đăng nhập"
        />
      </main>
    );
  const submit = async (e) => {
    e.preventDefault();
    setBusy(true);
    try {
      const o = await createOrder(form);
      onChange();
      nav("/don-hang/" + o.id);
    } catch (e) {
      setToast(e.message);
    } finally {
      setBusy(false);
    }
  };
  return (
    <main className="shell py-10">
      <Title
        eyebrow="Bước cuối cùng"
        title="Thông tin đặt hàng"
        description="Xác nhận địa chỉ nhận và phương thức thanh toán."
      />
      <form onSubmit={submit} className="grid gap-7 lg:grid-cols-[1fr_350px]">
        <section className="grid content-start gap-5">
          <div className="rounded-2xl border border-line p-6">
            <h2 className="mb-5 font-display font-extrabold">
              01 · Thông tin nhận hàng
            </h2>
            <label className={label}>Địa chỉ giao hàng</label>
            <textarea
              required
              maxLength={500}
              rows={3}
              className={input}
              placeholder="Số nhà, đường, phường/xã, quận/huyện, tỉnh/thành phố"
              value={form.shippingAddress}
              onChange={(e) =>
                setForm({
                  ...form,
                  shippingAddress: e.target.value,
                })
              }
            />
            <label className={label + " mt-5"}>Điện thoại</label>
            <input
              required
              className={input}
              value={form.phone}
              onChange={(e) =>
                setForm({
                  ...form,
                  phone: e.target.value,
                })
              }
              placeholder="09xx xxx xxx"
            />
          </div>
          <div className="rounded-2xl border border-line p-6">
            <h2 className="mb-5 font-display font-extrabold">
              02 · Phương thức thanh toán
            </h2>
            {[
              ["COD", "Thanh toán khi nhận hàng"],
              ["BANKING", "Chuyển khoản ngân hàng"],
            ].map(([key, title]) => (
              <label
                key={key}
                className="mb-3 flex cursor-pointer items-center gap-3 rounded-xl border border-line p-4"
              >
                <input
                  type="radio"
                  name="pay"
                  checked={form.paymentMethod === key}
                  onChange={() =>
                    setForm({
                      ...form,
                      paymentMethod: key,
                    })
                  }
                />
                <span className="text-sm font-bold">{title}</span>
              </label>
            ))}
            {form.paymentMethod === "BANKING" && (
              <p className="text-xs leading-5 text-amber-800">
                Cửa hàng sẽ liên hệ hướng dẫn chuyển khoản sau khi xác nhận đơn.
              </p>
            )}
          </div>
        </section>
        <aside className="h-fit rounded-2xl border border-line p-6">
          <h2 className="font-display font-extrabold">Đơn hàng của bạn</h2>
          {error ? (
            <p className="mt-4 text-sm text-rose-700">{error}</p>
          ) : !cart ? (
            <p className="mt-4 text-sm text-slate-400">Đang tải…</p>
          ) : (
            <>
              <div className="mt-5 grid gap-4">
                {cart.items?.map((i) => (
                  <div
                    key={i.id}
                    className="flex justify-between gap-3 text-xs"
                  >
                    <span>
                      {i.productName} · {i.size} × {i.quantity}
                    </span>
                    <b>{money(i.subTotal)}</b>
                  </div>
                ))}
              </div>
              <div className="mt-5 flex justify-between border-t border-line pt-4 font-extrabold">
                <span>Tổng thanh toán</span>
                <b className="text-brand-800">{money(cart.totalAmount)}</b>
              </div>
              <button
                disabled={busy || !cart.items?.length}
                className={primary + " mt-5 w-full"}
              >
                {busy ? "Đang tạo đơn…" : "Xác nhận đặt hàng"}{" "}
                <ArrowRight size={15} />
              </button>
              <p className="mt-3 text-center text-[10px] text-slate-400">
                Tồn kho được kiểm tra khi tạo đơn.
              </p>
            </>
          )}
        </aside>
      </form>
    </main>
  );
}

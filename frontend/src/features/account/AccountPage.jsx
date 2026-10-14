import { Link } from "react-router-dom";
import { Bell, LogOut, Package, ShoppingBag } from "lucide-react";
import { secondary } from "../../shared/uiStyles.js";
import { Title } from "../../components/ui/PageTitle.jsx";

export function Account({ user, logout }) {
  return (
    <main className="shell py-12">
      <Title
        eyebrow="Xin chào"
        title={user.fullName || user.email}
        description="Quản lý hoạt động mua sắm."
      />
      <div className="grid gap-4 sm:grid-cols-3">
        {[
          [
            "Đơn hàng của tôi",
            "Theo dõi trạng thái đơn hàng.",
            "/don-hang",
            Package,
          ],
          ["Thông báo", "Cập nhật mới từ cửa hàng.", "/thong-bao", Bell],
          [
            "Tiếp tục mua sắm",
            "Khám phá sản phẩm mới.",
            "/san-pham",
            ShoppingBag,
          ],
        ].map(([t, d, to, I]) => (
          <Link key={to} to={to} className="rounded-2xl border border-line p-6">
            <I className="text-brand-700" />
            <h2 className="mt-4 font-bold">{t}</h2>
            <p className="mt-2 text-sm text-slate-500">{d}</p>
          </Link>
        ))}
      </div>
      <button onClick={logout} className={secondary + " mt-7 text-rose-600"}>
        <LogOut size={16} /> Đăng xuất
      </button>
    </main>
  );
}

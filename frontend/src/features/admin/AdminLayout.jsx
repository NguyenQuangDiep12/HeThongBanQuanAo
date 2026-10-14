import { NavLink } from "react-router-dom";
import {
  ArrowUpRight,
  LayoutDashboard,
  Layers3,
  Package,
  ShieldCheck,
  ShoppingBag,
} from "lucide-react";
import { cx } from "../../shared/uiStyles.js";

export function AdminLayout({ children }) {
  return (
    <main className="min-h-[70vh] bg-slate-50/80 py-6 sm:py-9">
      <div className="shell max-w-[1440px]">
        <div className="mb-6 flex flex-wrap items-center justify-between gap-3">
          <div>
            <p className="text-[10px] font-extrabold uppercase tracking-[.2em] text-brand-700">
              Blue Thread · Vận hành
            </p>
            <p className="mt-1 font-display text-xl font-extrabold tracking-tight text-ink sm:text-2xl">
              Không gian quản trị
            </p>
          </div>
          <span className="inline-flex items-center gap-2 rounded-full border border-emerald-200 bg-white px-3 py-2 text-xs font-bold text-emerald-700 shadow-sm">
            <ShieldCheck size={15} /> Quyền quản trị
          </span>
        </div>

        <div className="grid items-start gap-5 lg:grid-cols-[248px_minmax(0,1fr)] lg:gap-7">
          <aside className="h-fit rounded-3xl border border-slate-200 bg-white p-3 shadow-sm lg:sticky lg:top-24">
            <div className="hidden rounded-2xl bg-gradient-to-br from-brand-800 to-brand-600 p-4 text-white lg:block">
              <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-white/15">
                <ShieldCheck size={20} />
              </div>
              <p className="mt-4 text-sm font-extrabold">Bảng điều khiển</p>
              <p className="mt-1 text-xs leading-5 text-blue-100">
                Quản lý cửa hàng trong một nơi.
              </p>
            </div>

            <p className="px-3 pb-2 pt-2 text-[10px] font-extrabold uppercase tracking-[.16em] text-slate-400 lg:pt-5">
              Danh mục quản lý
            </p>
            <nav aria-label="Điều hướng quản trị" className="scrollbar-hide flex gap-1 overflow-x-auto lg:block lg:overflow-visible">
              {[
                ["Tổng quan", "/admin", LayoutDashboard],
                ["Danh mục", "/admin/danh-muc", Layers3],
                ["Sản phẩm", "/admin/san-pham", ShoppingBag],
                ["Đơn hàng", "/admin/don-hang", Package],
              ].map(([title, to, Icon]) => (
                <NavLink
                  end={to === "/admin"}
                  key={to}
                  to={to}
                  className={({ isActive }) =>
                    cx(
                      "group mb-1 flex shrink-0 items-center gap-3 rounded-xl px-3 py-3 text-sm font-bold transition-colors",
                      isActive
                        ? "bg-brand-50 text-brand-800 ring-1 ring-inset ring-brand-100"
                        : "text-slate-500 hover:bg-slate-50 hover:text-slate-900",
                    )
                  }
                >
                  <Icon size={17} className="shrink-0" />
                  <span>{title}</span>
                  <ArrowUpRight size={14} className="ml-auto hidden text-brand-500 group-[.active]:block lg:block lg:opacity-0 lg:group-hover:opacity-100" />
                </NavLink>
              ))}
            </nav>

            <div className="mt-4 hidden rounded-2xl border border-slate-100 bg-slate-50 p-3 lg:block">
              <p className="text-xs font-bold text-slate-700">Cần thao tác nhanh?</p>
              <p className="mt-1 text-xs leading-5 text-slate-500">
                Chọn mục bên trên để quản lý danh mục, hàng hóa hoặc đơn hàng.
              </p>
            </div>
          </aside>

          <section className="min-w-0">{children}</section>
        </div>
      </div>
    </main>
  );
}

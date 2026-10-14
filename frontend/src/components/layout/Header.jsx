import { useEffect, useState } from "react";
import { Link, NavLink, useLocation, useNavigate } from "react-router-dom";
import {
  Bell,
  LogOut,
  Menu,
  Search,
  ShoppingCart,
  Sparkles,
  UserRound,
} from "lucide-react";

export function Header({ user, count, logout }) {
  const [open, setOpen] = useState(false),
    [term, setTerm] = useState("");
  const loc = useLocation();
  const navigate = useNavigate();
  const submitSearch = (event) => {
    event.preventDefault();
    const keyword = term.trim();
    navigate(keyword ? "/san-pham?keyword=" + encodeURIComponent(keyword) : "/san-pham");
    setOpen(false);
  };
  useEffect(() => setOpen(false), [loc.pathname]);
  return (
    <>
      <div className="bg-brand-800 py-2 text-center text-[10px] font-bold tracking-[.12em] text-white">
        <Sparkles size={13} className="mr-2 inline" /> ĐẶT HÀNG TRỰC TUYẾN ·
        THEO DÕI TRẠNG THÁI ĐƠN HÀNG
      </div>
      <header className="sticky top-0 z-40 border-b border-line bg-white/95 backdrop-blur">
        <div className="shell flex h-[74px] items-center justify-between gap-4">
          <button
            type="button"
            aria-label={open ? "Đóng menu" : "Mở menu"}
            aria-expanded={open}
            className="rounded-lg p-2 text-slate-700 transition hover:bg-slate-100 lg:hidden"
            onClick={() => setOpen(!open)}
          >
            <Menu />
          </button>
          <Link
            to="/"
            className="shrink-0 font-display text-xl font-extrabold tracking-[-.08em] text-brand-800"
          >
            BLUE<span className="text-ink">THREAD</span>
            <i className="text-brand-500">.</i>
          </Link>
          <nav className="hidden gap-6 text-xs font-bold text-slate-600 lg:flex">
            <NavLink to="/" className={({ isActive }) => isActive ? "text-brand-800" : "transition hover:text-brand-700"}>Trang chủ</NavLink>
            <NavLink to="/san-pham" className={({ isActive }) => isActive ? "text-brand-800" : "transition hover:text-brand-700"}>Sản phẩm</NavLink>
            <Link className="transition hover:text-brand-700" to="/san-pham?sort=newest">Hàng mới</Link>
            <Link className="transition hover:text-brand-700" to="/san-pham?inStock=true">Có sẵn</Link>
            {user?.role === "ADMIN" && (
              <Link to="/admin" className="text-brand-700">
                Quản trị
              </Link>
            )}
          </nav>
          <form
            className="hidden max-w-[280px] flex-1 lg:block"
            onSubmit={submitSearch}
          >
            <div className="flex items-center gap-2 rounded-xl bg-slate-50 px-3">
              <Search size={16} className="text-slate-400" />
              <input
                value={term}
                onChange={(e) => setTerm(e.target.value)}
                className="h-10 w-full bg-transparent text-xs outline-none"
                placeholder="Tìm sản phẩm..."
              />
            </div>
          </form>
          <div className="flex shrink-0 items-center gap-3">
            <Link
              to={user?.role === "ADMIN" ? "/admin" : user ? "/tai-khoan" : "/dang-nhap"}
              className="hidden items-center gap-2 rounded-lg px-2 py-2 text-xs font-bold sm:flex"
            >
              <UserRound size={18} />
              {user?.fullName?.split(" ")[0] || "Tài khoản"}
            </Link>
            {user?.role === "USER" && (
              <Link
                to="/thong-bao"
                aria-label="Thông báo"
                className="hidden rounded-lg p-2 text-slate-600 transition hover:bg-slate-100 hover:text-brand-700 sm:block"
              >
                <Bell size={18} />
              </Link>
            )}
            {user?.role !== "ADMIN" && (
              <Link
                to="/gio-hang"
                aria-label="Giỏ hàng"
                className="relative rounded-lg p-2 text-slate-600 transition hover:bg-slate-100 hover:text-brand-700"
              >
                <ShoppingCart size={20} />
                {count > 0 && (
                  <b className="absolute right-0 top-0 grid h-4 min-w-4 place-items-center rounded-full bg-brand-700 px-1 text-[9px] text-white">
                    {count}
                  </b>
                )}
              </Link>
            )}
            {user && (
              <button
                type="button"
                onClick={logout}
                className="hidden items-center gap-2 rounded-lg border border-slate-200 px-3 py-2 text-xs font-bold text-slate-600 transition hover:border-rose-200 hover:bg-rose-50 hover:text-rose-700 sm:inline-flex"
              >
                <LogOut size={16} />
                Đăng xuất
              </button>
            )}
          </div>
        </div>
        {open && (
          <div className="border-t p-4 lg:hidden">
            <form
              onSubmit={submitSearch}
              className="mb-3 flex rounded-xl bg-slate-50 px-3"
            >
              <Search size={16} />
              <input
                value={term}
                onChange={(e) => setTerm(e.target.value)}
                className="h-11 flex-1 bg-transparent px-2 text-sm outline-none"
                placeholder="Tìm sản phẩm..."
              />
            </form>
            <div className="grid gap-2 text-sm font-bold">
              <Link to="/">Trang chủ</Link>
              <Link to="/san-pham">Tất cả sản phẩm</Link>
              <Link to={user?.role === "ADMIN" ? "/admin" : user ? "/tai-khoan" : "/dang-nhap"}>
                {user?.role === "ADMIN" ? "Quản trị" : "Tài khoản"}
              </Link>
              {user && (
                <button onClick={logout} className="text-left text-rose-600">
                  Đăng xuất
                </button>
              )}
            </div>
          </div>
        )}
      </header>
    </>
  );
}

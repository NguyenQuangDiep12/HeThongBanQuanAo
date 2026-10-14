import { useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { ArrowRight, LockKeyhole } from "lucide-react";
import { saveSession } from "../../api";
import { pics } from "../../shared/media.js";
import { primary, input, label } from "../../shared/uiStyles.js";
import { registerAccount, loginAccount } from "../../services/authService.js";

export function Auth({ setToast, onLogin }) {
  const [mode, setMode] = useState("login"),
    [form, setForm] = useState({
      fullName: "",
      email: "",
      password: "",
      phone: "",
    }),
    [busy, setBusy] = useState(false),
    nav = useNavigate(),
    loc = useLocation();
  const submit = async (e) => {
    e.preventDefault();
    setBusy(true);
    try {
      if (mode === "register") {
        await registerAccount(form);
        setToast("Tạo tài khoản thành công. Vui lòng đăng nhập.");
        setMode("login");
      } else {
        const r = await loginAccount({
            email: form.email,
            password: form.password,
          }),
          role = String(r.role || "").replace(/^ROLE_/i, "").toUpperCase(),
          u = {
            id: r.userId,
            email: form.email,
            role,
            fullName: form.email.split("@")[0],
          };
        saveSession(r.accessToken, u);
        onLogin(u);
        const redirectTarget = new URLSearchParams(loc.search).get("redirect");
        const safeRedirect = redirectTarget?.startsWith("/")
          && !redirectTarget.startsWith("//")
          ? redirectTarget
          : null;
        const isAdminRedirect = safeRedirect === "/admin"
          || safeRedirect?.startsWith("/admin/");
        const destination = role === "ADMIN"
          ? isAdminRedirect ? safeRedirect : "/admin"
          : safeRedirect && !isAdminRedirect ? safeRedirect : "/tai-khoan";
        nav(destination);
      }
    } catch (e) {
      setToast(e.message);
    } finally {
      setBusy(false);
    }
  };
  return (
    <main className="shell grid min-h-[620px] items-center gap-12 py-10 lg:grid-cols-2">
      <div className="relative hidden h-[540px] overflow-hidden rounded-[28px] lg:block">
        <img src={pics.men} className="h-full w-full object-cover" />
        <div className="absolute inset-0 bg-gradient-to-t from-[#102340]/90 to-transparent" />
        <h2 className="absolute bottom-10 left-10 font-display text-4xl font-extrabold text-white">
          Định hình phong cách
          <br />
          theo cách của bạn.
        </h2>
      </div>
      <div className="mx-auto w-full max-w-md">
        <Link to="/" className="text-xs font-bold text-slate-400">
          ← Quay về cửa hàng
        </Link>
        <p className="mt-8 text-[10px] font-extrabold uppercase tracking-widest text-brand-700">
          {mode === "login" ? "Chào mừng trở lại" : "Tham gia cùng chúng tôi"}
        </p>
        <h1 className="mt-2 font-display text-3xl font-extrabold">
          {mode === "login" ? "Đăng nhập" : "Tạo tài khoản"}
        </h1>
        <form onSubmit={submit} className="mt-7 grid gap-4">
          {mode === "register" && (
            <div>
              <label className={label}>Họ và tên</label>
              <input
                required
                className={input}
                value={form.fullName}
                onChange={(e) =>
                  setForm({
                    ...form,
                    fullName: e.target.value,
                  })
                }
              />
            </div>
          )}
          <div>
            <label className={label}>Email</label>
            <input
              required
              type="email"
              className={input}
              value={form.email}
              onChange={(e) =>
                setForm({
                  ...form,
                  email: e.target.value,
                })
              }
            />
          </div>
          {mode === "register" && (
            <div>
              <label className={label}>Số điện thoại</label>
              <input
                className={input}
                value={form.phone}
                onChange={(e) =>
                  setForm({
                    ...form,
                    phone: e.target.value,
                  })
                }
              />
            </div>
          )}
          <div>
            <label className={label}>Mật khẩu</label>
            <input
              required
              minLength={6}
              type="password"
              className={input}
              value={form.password}
              onChange={(e) =>
                setForm({
                  ...form,
                  password: e.target.value,
                })
              }
            />
          </div>
          <button disabled={busy} className={primary + " mt-2 w-full"}>
            {busy
              ? "Đang xử lý…"
              : mode === "login"
                ? "Đăng nhập"
                : "Tạo tài khoản"}{" "}
            <ArrowRight size={16} />
          </button>
        </form>
        <p className="mt-6 text-center text-sm text-slate-500">
          {mode === "login" ? "Chưa có tài khoản?" : "Đã có tài khoản?"}{" "}
          <button
            onClick={() => setMode(mode === "login" ? "register" : "login")}
            className="font-bold text-brand-700"
          >
            {mode === "login" ? "Đăng ký" : "Đăng nhập"}
          </button>
        </p>
        <p className="mt-6 text-center text-xs text-slate-400">
          <LockKeyhole size={13} className="mr-1 inline" /> Phiên đăng nhập bảo
          mật
        </p>
      </div>
    </main>
  );
}

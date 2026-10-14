import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import {
  ArrowRight,
  Boxes,
  CircleDollarSign,
  Clock3,
  Layers3,
  PackageCheck,
  Plus,
  ShoppingBag,
} from "lucide-react";
import { money } from "../../api";
import { date } from "../../shared/format.js";
import { Pill } from "../../components/ui/StatusPill.jsx";
import { Title } from "../../components/ui/PageTitle.jsx";
import {
  fetchAdminProducts,
  fetchAdminOrders,
} from "../../services/adminService.js";

export function AdminHome() {
  const [p, setP] = useState(null),
    [o, setO] = useState(null),
    [err, setErr] = useState(""),
    [loading, setLoading] = useState(true),
    [reloadKey, setReloadKey] = useState(0);

  useEffect(() => {
    let active = true;
    setLoading(true);
    setErr("");
    Promise.all([fetchAdminProducts(), fetchAdminOrders()])
      .then(([a, b]) => {
        if (!active) return;
        setP(a);
        setO(b);
      })
      .catch((e) => {
        if (active) setErr(e.message);
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, [reloadKey]);

  const pendingOrders = o?.filter((order) => order.status === "PENDING") || [];
  const completedValue = o
    ?.filter((order) => order.status !== "CANCELLED")
    .reduce((total, order) => total + Number(order.totalAmount || 0), 0);

  return (
    <>
      <Title
        eyebrow="Bảng điều khiển"
        title="Xin chào, Admin"
        description="Tổng quan hoạt động và những việc cần xử lý tại cửa hàng."
      >
        <button
          type="button"
          onClick={() => setReloadKey((key) => key + 1)}
          disabled={loading}
          className="inline-flex items-center justify-center gap-2 rounded-xl border border-slate-200 bg-white px-4 py-2.5 text-xs font-bold text-slate-700 shadow-sm transition hover:border-brand-200 hover:text-brand-800 disabled:opacity-60"
        >
          {loading ? "Đang cập nhật…" : "Làm mới dữ liệu"}
        </button>
      </Title>

      <section className="relative mb-5 overflow-hidden rounded-3xl bg-gradient-to-br from-brand-800 via-brand-700 to-blue-500 p-5 text-white shadow-lg shadow-blue-900/10 sm:p-7">
        <div className="relative z-10 max-w-xl">
          <p className="text-xs font-bold uppercase tracking-[.16em] text-blue-100">
            Cửa hàng của bạn
          </p>
          <h2 className="mt-2 font-display text-2xl font-extrabold tracking-tight sm:text-3xl">
            Mọi thứ đang trong tầm tay.
          </h2>
          <p className="mt-2 max-w-lg text-sm leading-6 text-blue-100">
            Theo dõi đơn mới, cập nhật sản phẩm và giữ cho danh mục luôn sẵn
            sàng phục vụ khách hàng.
          </p>
          <div className="mt-5 flex flex-wrap gap-2.5">
            <Link
              to="/admin/don-hang"
              className="inline-flex items-center gap-2 rounded-xl bg-white px-4 py-2.5 text-xs font-extrabold !text-brand-800 transition hover:bg-blue-50"
            >
              Xem đơn hàng <ArrowRight size={15} />
            </Link>
            <Link
              to="/admin/san-pham"
              className="inline-flex items-center gap-2 rounded-xl border border-white/30 bg-white/10 px-4 py-2.5 text-xs font-bold text-white transition hover:bg-white/20"
            >
              <Plus size={15} /> Thêm sản phẩm
            </Link>
          </div>
        </div>
        <div className="pointer-events-none absolute -right-5 -top-12 hidden h-64 w-64 rounded-full border-[36px] border-white/10 sm:block" />
        <div className="pointer-events-none absolute -bottom-28 right-20 hidden h-64 w-64 rounded-full border-[36px] border-white/10 sm:block" />
      </section>

      {err && (
        <div role="alert" className="mb-5 flex flex-wrap items-center justify-between gap-3 rounded-2xl border border-rose-200 bg-rose-50 p-4 text-sm text-rose-800">
          <span>{err}</span>
          <button
            type="button"
            onClick={() => setReloadKey((key) => key + 1)}
            className="rounded-lg bg-white px-3 py-2 text-xs font-bold text-rose-800 shadow-sm"
          >
            Thử tải lại
          </button>
        </div>
      )}

      <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
        <MetricCard
          title="Sản phẩm"
          value={loading ? null : p?.length ?? 0}
          hint="Đang quản lý trong cửa hàng"
          Icon={ShoppingBag}
          tone="blue"
        />
        <MetricCard
          title="Tổng đơn hàng"
          value={loading ? null : o?.length ?? 0}
          hint="Tất cả trạng thái"
          Icon={PackageCheck}
          tone="violet"
        />
        <MetricCard
          title="Chờ xác nhận"
          value={loading ? null : pendingOrders.length}
          hint="Đơn cần được xử lý"
          Icon={Clock3}
          tone="amber"
          attention={pendingOrders.length > 0}
        />
        <MetricCard
          title="Giá trị đơn"
          value={loading ? null : money(completedValue)}
          hint="Đơn không bao gồm đơn đã hủy"
          Icon={CircleDollarSign}
          tone="emerald"
        />
      </div>

      <section className="mt-6 grid gap-5 xl:grid-cols-[minmax(0,1.5fr)_minmax(260px,.8fr)]">
        <div className="overflow-hidden rounded-3xl border border-slate-200 bg-white shadow-sm">
          <div className="flex flex-wrap items-center justify-between gap-3 border-b border-slate-100 px-5 py-4 sm:px-6">
            <div>
              <h2 className="font-display text-base font-extrabold text-ink">Đơn hàng gần đây</h2>
              <p className="mt-1 text-xs text-slate-500">Theo dõi các đơn vừa phát sinh</p>
            </div>
            <Link to="/admin/don-hang" className="inline-flex items-center gap-1 text-xs font-extrabold text-brand-700 hover:text-brand-800">
              Tất cả đơn <ArrowRight size={14} />
            </Link>
          </div>

          {loading ? (
            <div className="space-y-4 p-5 sm:p-6" aria-label="Đang tải đơn hàng">
              {[0, 1, 2].map((row) => <div key={row} className="h-12 animate-pulse rounded-xl bg-slate-100" />)}
            </div>
          ) : !o?.length ? (
            <div className="px-5 py-12 text-center sm:px-6">
              <div className="mx-auto grid h-12 w-12 place-items-center rounded-2xl bg-brand-50 text-brand-700"><PackageCheck size={21} /></div>
              <h3 className="mt-3 text-sm font-extrabold text-ink">Chưa có đơn hàng</h3>
              <p className="mt-1 text-xs text-slate-500">Đơn hàng mới sẽ xuất hiện tại đây.</p>
            </div>
          ) : (
            <div className="divide-y divide-slate-100">
              {o.slice(0, 6).map((order) => (
                <div key={order.id} className="flex flex-col gap-2 px-5 py-4 transition hover:bg-slate-50/80 sm:flex-row sm:items-center sm:justify-between sm:px-6">
                  <div className="min-w-0">
                    <p className="text-sm font-extrabold text-ink">Đơn hàng #BT-{order.id}</p>
                    <p className="mt-1 text-xs text-slate-500">{date(order.createdAt)}</p>
                  </div>
                  <div className="flex items-center justify-between gap-4 sm:justify-end">
                    <Pill status={order.status} />
                    <b className="min-w-28 text-right text-sm text-ink">{money(order.totalAmount)}</b>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

        <aside className="rounded-3xl border border-slate-200 bg-white p-5 shadow-sm sm:p-6">
          <div>
            <h2 className="font-display text-base font-extrabold text-ink">Truy cập nhanh</h2>
            <p className="mt-1 text-xs text-slate-500">Các khu vực thường dùng</p>
          </div>
          <div className="mt-4 space-y-2.5">
            <QuickLink to="/admin/danh-muc" Icon={Layers3} title="Quản lý danh mục" description="Sắp xếp nhóm sản phẩm" />
            <QuickLink to="/admin/san-pham" Icon={Boxes} title="Quản lý sản phẩm" description="Thông tin, biến thể và tồn kho" />
            <QuickLink to="/admin/don-hang" Icon={PackageCheck} title="Xử lý đơn hàng" description="Xác nhận và cập nhật giao hàng" />
          </div>
          <div className="mt-5 rounded-2xl bg-slate-50 p-4">
            <p className="text-xs font-extrabold text-slate-700">Gợi ý vận hành</p>
            <p className="mt-1 text-xs leading-5 text-slate-500">
              Kiểm tra đơn chờ xác nhận thường xuyên để khách nhận được phản hồi sớm.
            </p>
          </div>
        </aside>
      </section>
      <p className="mt-4 text-[11px] leading-5 text-slate-400">
        Các chỉ số được tổng hợp từ dữ liệu hiện có trên hệ thống.
      </p>
    </>
  );
}

function MetricCard({ title, value, hint, Icon, tone, attention = false }) {
  const tones = {
    blue: "bg-blue-50 text-blue-700",
    violet: "bg-violet-50 text-violet-700",
    amber: "bg-amber-50 text-amber-700",
    emerald: "bg-emerald-50 text-emerald-700",
  };

  return (
    <article className={`rounded-2xl border bg-white p-4 shadow-sm transition hover:-translate-y-0.5 hover:shadow-md sm:p-5 ${attention ? "border-amber-200" : "border-slate-200"}`}>
      <div className="flex items-start justify-between gap-3">
        <div>
          <p className="text-xs font-bold text-slate-500">{title}</p>
          {value == null ? (
            <div className="mt-3 h-8 w-24 animate-pulse rounded-lg bg-slate-100" />
          ) : (
            <p className="mt-2 font-display text-2xl font-extrabold tracking-tight text-ink">{value}</p>
          )}
        </div>
        <span className={`grid h-10 w-10 place-items-center rounded-xl ${tones[tone]}`}><Icon size={19} /></span>
      </div>
      <p className="mt-3 text-[11px] text-slate-400">{hint}</p>
    </article>
  );
}

function QuickLink({ to, Icon, title, description }) {
  return (
    <Link to={to} className="group flex items-center gap-3 rounded-2xl border border-slate-100 p-3 transition hover:border-brand-200 hover:bg-brand-50/60">
      <span className="grid h-10 w-10 shrink-0 place-items-center rounded-xl bg-slate-100 text-slate-600 transition group-hover:bg-white group-hover:text-brand-700"><Icon size={18} /></span>
      <span className="min-w-0 flex-1">
        <span className="block text-xs font-extrabold text-slate-800">{title}</span>
        <span className="mt-1 block text-[11px] text-slate-500">{description}</span>
      </span>
      <ArrowRight size={15} className="shrink-0 text-slate-300 transition group-hover:translate-x-0.5 group-hover:text-brand-700" />
    </Link>
  );
}

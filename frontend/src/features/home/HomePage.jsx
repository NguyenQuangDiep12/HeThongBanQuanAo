import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { ArrowRight, Package } from "lucide-react";
import { pics } from "../../shared/media.js";
import { btn, secondary } from "../../shared/uiStyles.js";
import { Title } from "../../components/ui/PageTitle.jsx";
import { Empty } from "../../components/ui/EmptyState.jsx";
import { Grid } from "../../components/products/ProductGrid.jsx";
import {
  fetchFeaturedProducts,
  fetchProductCategories,
} from "../../services/productService.js";

export function Home() {
  const [data, setData] = useState(null),
    [cats, setCats] = useState([]),
    [error, setError] = useState(""),
    [loading, setLoading] = useState(true),
    [reloadKey, setReloadKey] = useState(0);

  useEffect(() => {
    let active = true;
    setLoading(true);
    setError("");
    fetchFeaturedProducts()
      .then((products) => {
        if (active) setData(products);
      })
      .catch((e) => {
        if (active) setError(e.message);
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    fetchProductCategories()
      .then((categories) => {
        if (active) setCats(categories);
      })
      .catch(() => {});
    return () => {
      active = false;
    };
  }, [reloadKey]);
  return (
    <>
      <section className="shell pt-6">
        <div className="relative min-h-[450px] overflow-hidden rounded-[28px] bg-slate-200">
          <img
            src={pics.hero}
            className="absolute inset-0 h-full w-full object-cover"
            alt="Bộ sưu tập thời trang"
          />
          <div className="absolute inset-0 bg-gradient-to-r from-[#102340]/85 via-[#102340]/45 to-transparent" />
          <div className="relative flex min-h-[450px] max-w-xl flex-col justify-center px-7 py-12 text-white sm:px-14">
            <span className="w-fit rounded-full border border-white/30 bg-white/10 px-4 py-2 text-[10px] font-bold uppercase tracking-widest">
              Bộ sưu tập 2026
            </span>
            <h1 className="mt-6 font-display text-5xl font-extrabold leading-[1.05] tracking-[-.06em] sm:text-6xl">
              Mặc điều
              <br />
              bạn <span className="text-blue-200">cảm thấy.</span>
            </h1>
            <p className="mt-5 max-w-sm text-sm leading-7 text-white/75">
              Thiết kế tinh giản, phom dáng thoải mái và sắc xanh cho nhịp sống
              hiện đại.
            </p>
            <Link
              to="/san-pham"
              className={btn + " mt-7 w-fit bg-white !text-brand-800 shadow-lg shadow-slate-950/15 hover:bg-blue-50"}
            >
              Khám phá sản phẩm <ArrowRight size={16} />
            </Link>
          </div>
        </div>
      </section>
      <section className="shell py-16">
        <Title eyebrow="Chọn theo phong cách" title="Khám phá danh mục">
          <Link to="/san-pham" className="text-xs font-bold text-brand-700">
            Tất cả sản phẩm →
          </Link>
        </Title>
        <div className="grid gap-4 sm:grid-cols-3">
          {[cats[0] || "Nữ", cats[1] || "Nam", cats[2] || "Phụ kiện"].map(
            (c, i) => (
              <Link
                to={"/san-pham?category=" + encodeURIComponent(c)}
                key={i}
                className="group relative h-64 overflow-hidden rounded-2xl"
              >
                <img
                  src={i === 1 ? pics.men : pics.women}
                  className="h-full w-full object-cover transition group-hover:scale-105"
                  alt={c}
                />
                <div className="absolute inset-0 bg-gradient-to-t from-black/65 to-transparent" />
                <b className="absolute bottom-5 left-5 font-display text-xl text-white">
                  {c} <ArrowRight className="inline" size={16} />
                </b>
              </Link>
            ),
          )}
        </div>
      </section>
      <section className="bg-canvas py-16">
        <div className="shell">
          <Title
            eyebrow="Được tuyển chọn"
            title="Sản phẩm mới nhất"
            description="Những lựa chọn mới cho tủ đồ của bạn."
          >
            <Link to="/san-pham" className={secondary}>
              Xem tất cả <ArrowRight size={15} />
            </Link>
          </Title>
          {error ? (
            <div role="alert" className="flex flex-wrap items-center justify-between gap-3 rounded-2xl border border-amber-200 bg-amber-50 p-4 text-sm text-amber-900">
              <p>{error} · Hãy kiểm tra kết nối rồi thử lại.</p>
              <button
                type="button"
                onClick={() => setReloadKey((key) => key + 1)}
                className="rounded-lg bg-white px-3 py-2 text-xs font-bold text-amber-900 shadow-sm"
              >
                Thử tải lại
              </button>
            </div>
          ) : loading ? (
            <div className="grid grid-cols-2 gap-x-4 gap-y-8 sm:grid-cols-3 lg:grid-cols-4 lg:gap-6" aria-label="Đang tải sản phẩm">
              {[0, 1, 2, 3].map((item) => (
                <div key={item} className="animate-pulse">
                  <div className="aspect-[.82] rounded-2xl bg-slate-200" />
                  <div className="mt-4 h-3 w-20 rounded bg-slate-200" />
                  <div className="mt-2 h-4 w-3/4 rounded bg-slate-200" />
                  <div className="mt-3 h-4 w-1/2 rounded bg-slate-200" />
                </div>
              ))}
            </div>
          ) : data?.items?.length ? (
            <Grid items={data.items} />
          ) : (
            <Empty
              Icon={Package}
              title="Chưa có sản phẩm"
              body="Sản phẩm sẽ hiển thị tại đây khi được thêm vào cửa hàng."
            />
          )}
        </div>
      </section>
      <section className="shell py-16">
        <div className="rounded-[28px] bg-brand-800 p-8 text-white sm:flex sm:items-center sm:justify-between sm:p-12">
          <div>
            <p className="text-[10px] font-bold uppercase tracking-widest text-blue-200">
              Chăm chút từng trải nghiệm
            </p>
            <h2 className="mt-3 font-display text-3xl font-extrabold">
              Tủ đồ tốt bắt đầu từ
              <br /> những món đồ đúng.
            </h2>
          </div>
          <Link
            to="/san-pham"
            className={btn + " mt-6 bg-white !text-brand-800 shadow-lg shadow-slate-950/10 transition hover:bg-blue-50 sm:mt-0"}
          >
            Chọn đồ ngay <ArrowRight size={15} />
          </Link>
        </div>
      </section>
    </>
  );
}

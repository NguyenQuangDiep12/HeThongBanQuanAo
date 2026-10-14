import { useEffect, useState } from "react";
import { useSearchParams } from "react-router-dom";
import {
  ChevronLeft,
  ChevronRight,
  Search,
  SlidersHorizontal,
  X,
} from "lucide-react";
import { cx, primary, secondary, input } from "../../shared/uiStyles.js";
import { Title } from "../../components/ui/PageTitle.jsx";
import { Empty } from "../../components/ui/EmptyState.jsx";
import { Grid } from "../../components/products/ProductGrid.jsx";
import {
  fetchProductFilters,
  searchProducts,
} from "../../services/productService.js";

export function Catalog() {
  const [params, setParams] = useSearchParams(),
    [filters, setFilters] = useState(null),
    [data, setData] = useState(null),
    [error, setError] = useState(""),
    [loading, setLoading] = useState(true),
    [mobile, setMobile] = useState(false);
  const val = (k) => params.get(k) || "",
    pg = Number(val("page") || 0);
  useEffect(() => {
    fetchProductFilters()
      .then(setFilters)
      .catch(() => {});
  }, []);
  useEffect(() => {
    let live = true;
    setLoading(true);
    setError("");
    const q = new URLSearchParams(params);
    q.set("pageSize", "12");
    searchProducts(q)
      .then((d) => live && setData(d))
      .catch((e) => live && setError(e.message))
      .finally(() => live && setLoading(false));
    return () => {
      live = false;
    };
  }, [params.toString()]);
  const set = (k, v) => {
    let p = new URLSearchParams(params);
    v ? p.set(k, v) : p.delete(k);
    p.delete("page");
    setParams(p);
  };
  const clear = () =>
    setParams(
      val("keyword")
        ? {
            keyword: val("keyword"),
          }
        : {},
    );
  const filtersUi = (
    <>
      <div className="mb-6">
        <b className="text-sm">Danh mục</b>
        <div className="mt-3 grid gap-2">
          {(filters?.categories || []).map((c) => (
            <button
              key={c}
              onClick={() => set("category", val("category") === c ? "" : c)}
              className={cx(
                "text-left text-sm",
                val("category") === c
                  ? "font-bold text-brand-700"
                  : "text-slate-500",
              )}
            >
              {c}
            </button>
          ))}
        </div>
      </div>
      <div className="mb-6">
        <b className="text-sm">Kích cỡ</b>
        <div className="mt-3 flex flex-wrap gap-2">
          {(filters?.sizes || []).map((s) => (
            <button
              key={s}
              onClick={() => set("size", val("size") === s ? "" : s)}
              className={cx(
                "rounded-lg border px-3 py-2 text-xs font-bold",
                val("size") === s ? "bg-brand-800 text-white" : "border-line",
              )}
            >
              {s}
            </button>
          ))}
        </div>
      </div>
      <div className="mb-6">
        <b className="text-sm">Màu sắc</b>
        <select
          className={input + " mt-3"}
          value={val("color")}
          onChange={(e) => set("color", e.target.value)}
        >
          <option value="">Tất cả màu</option>
          {(filters?.colors || []).map((c) => (
            <option key={c}>{c}</option>
          ))}
        </select>
      </div>
      <div className="mb-6">
        <b className="text-sm">Khoảng giá (VND)</b>
        <div className="mt-3 flex gap-2">
          <input
            className={input + " !px-2"}
            type="number"
            placeholder="Từ"
            value={val("minPrice")}
            onChange={(e) => set("minPrice", e.target.value)}
          />
          <input
            className={input + " !px-2"}
            type="number"
            placeholder="Đến"
            value={val("maxPrice")}
            onChange={(e) => set("maxPrice", e.target.value)}
          />
        </div>
      </div>
      <label className="flex gap-2 border-t border-line pt-4 text-sm">
        <input
          type="checkbox"
          checked={val("inStock") === "true"}
          onChange={(e) => set("inStock", e.target.checked ? "true" : "")}
        />{" "}
        Chỉ xem hàng có sẵn
      </label>
    </>
  );
  return (
    <main className="shell py-10">
      <Title
        eyebrow="Cửa hàng BLUE THREAD"
        title="Tất cả sản phẩm"
        description={
          val("keyword")
            ? "Kết quả tìm kiếm: " + val("keyword")
            : "Tìm món đồ phù hợp với phong cách của bạn."
        }
      >
        <button
          onClick={() => setMobile(true)}
          className={secondary + " lg:hidden"}
        >
          <SlidersHorizontal size={15} /> Bộ lọc
        </button>
      </Title>
      <div className="grid gap-8 lg:grid-cols-[220px_1fr]">
        <aside className="hidden h-fit rounded-2xl border border-line p-5 lg:block">
          <div className="mb-5 flex justify-between">
            <b>Bộ lọc</b>
            <button
              onClick={clear}
              className="text-xs font-bold text-brand-700"
            >
              Xóa lọc
            </button>
          </div>
          {filtersUi}
        </aside>
        <section>
          <div className="mb-5 flex items-center justify-between border-b border-line pb-4 text-xs text-slate-500">
            <span>
              {loading ? "Đang tải…" : (data?.totalElements || 0) + " sản phẩm"}
            </span>
            <label>
              Sắp xếp{" "}
              <select
                className="ml-2 rounded-lg border border-line p-2 font-bold text-ink"
                value={val("sort") || "newest"}
                onChange={(e) => set("sort", e.target.value)}
              >
                <option value="newest">Mới nhất</option>
                <option value="price_asc">Giá tăng dần</option>
                <option value="price_desc">Giá giảm dần</option>
                <option value="name_asc">Tên A–Z</option>
              </select>
            </label>
          </div>
          {error ? (
            <Empty
              Icon={Search}
              title="Không tải được sản phẩm"
              body={error + " Kiểm tra backend đang chạy."}
              action="Thử lại"
              to="/san-pham"
            />
          ) : loading ? (
            <p className="py-16 text-center text-sm text-slate-400">
              Đang tải sản phẩm…
            </p>
          ) : data?.items?.length ? (
            <>
              <Grid items={data.items} />
              {data.totalPages > 1 && (
                <div className="mt-9 flex justify-center gap-4">
                  <button
                    disabled={data.first}
                    onClick={() => set("page", String(pg - 1))}
                    className={secondary}
                  >
                    <ChevronLeft size={16} />
                  </button>
                  <span className="self-center text-xs">
                    Trang {pg + 1} / {data.totalPages}
                  </span>
                  <button
                    disabled={data.last}
                    onClick={() => set("page", String(pg + 1))}
                    className={secondary}
                  >
                    <ChevronRight size={16} />
                  </button>
                </div>
              )}
            </>
          ) : (
            <Empty
              Icon={Search}
              title="Không tìm thấy sản phẩm"
              body="Thử đổi từ khóa hoặc xóa bớt bộ lọc."
              action="Xóa bộ lọc"
              to="/san-pham"
            />
          )}
        </section>
      </div>
      {mobile && (
        <div
          className="fixed inset-0 z-50 bg-black/40 lg:hidden"
          onClick={() => setMobile(false)}
        >
          <div
            className="absolute inset-y-0 right-0 w-[min(370px,90vw)] overflow-auto bg-white p-6"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="mb-5 flex justify-between">
              <b>Bộ lọc sản phẩm</b>
              <button onClick={() => setMobile(false)}>
                <X />
              </button>
            </div>
            {filtersUi}
            <button
              onClick={() => setMobile(false)}
              className={primary + " mt-6 w-full"}
            >
              Xem sản phẩm
            </button>
          </div>
        </div>
      )}
    </main>
  );
}

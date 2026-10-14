import { useEffect, useMemo, useState } from "react";
import { Archive, Boxes, Edit3, Plus, Search, Shirt } from "lucide-react";
import { money } from "../../api";
import { primary, input } from "../../shared/uiStyles.js";
import { Title } from "../../components/ui/PageTitle.jsx";
import {
  deactivateAdminProduct,
  fetchAdminCategories,
  fetchAdminProducts,
} from "../../services/adminService.js";
import { ProductFormDialog } from "./components/ProductFormDialog.jsx";
import { VariantManagerDialog } from "./components/VariantManagerDialog.jsx";

const statusLabel = {
  ACTIVE: "Đang kinh doanh",
  INACTIVE: "Đã ẩn",
};

export function AdminProducts({ setToast }) {
  const [products, setProducts] = useState(null);
  const [categories, setCategories] = useState([]);
  const [error, setError] = useState("");
  const [query, setQuery] = useState("");
  const [categoryFilter, setCategoryFilter] = useState("all");
  const [editingProduct, setEditingProduct] = useState(undefined);
  const [variantsProductId, setVariantsProductId] = useState(null);

  const load = () => Promise.all([
    fetchAdminProducts(),
    fetchAdminCategories(),
  ]).then(([productRows, categoryRows]) => {
    setProducts(productRows);
    setCategories(categoryRows);
    setError("");
  }).catch((loadError) => setError(loadError.message));

  useEffect(() => {
    let mounted = true;
    Promise.all([fetchAdminProducts(), fetchAdminCategories()])
      .then(([productRows, categoryRows]) => {
        if (!mounted) return;
        setProducts(productRows);
        setCategories(categoryRows);
      })
      .catch((loadError) => {
        if (mounted) setError(loadError.message);
      });
    return () => { mounted = false; };
  }, []);

  const filteredProducts = useMemo(() => {
    const normalizedQuery = query.trim().toLocaleLowerCase("vi");
    return (products || []).filter((product) => {
      const matchesQuery = !normalizedQuery
        || product.name.toLocaleLowerCase("vi").includes(normalizedQuery)
        || String(product.id).includes(normalizedQuery);
      const matchesCategory = categoryFilter === "all"
        || String(product.categoryId) === categoryFilter;
      return matchesQuery && matchesCategory;
    });
  }, [products, query, categoryFilter]);

  const variantsProduct = products?.find((product) => product.id === variantsProductId);

  const archiveProduct = async (product) => {
    if (!window.confirm(`Ẩn sản phẩm “${product.name}” khỏi cửa hàng?`)) return;
    try {
      await deactivateAdminProduct(product.id);
      setToast?.("Đã ẩn sản phẩm khỏi cửa hàng.");
      await load();
    } catch (actionError) {
      setToast?.(actionError.message);
    }
  };

  return (
    <>
      <Title
        eyebrow="Quản lý hàng hóa"
        title="Sản phẩm"
        description="Mỗi sản phẩm thuộc một danh mục và có thể chứa nhiều biến thể size, màu sắc, giá và tồn kho."
      >
        <button
          onClick={() => setEditingProduct(null)}
          disabled={!categories.some((category) => category.active)}
          className={primary}
        >
          <Plus size={16} /> Thêm sản phẩm
        </button>
      </Title>

      {!categories.some((category) => category.active) && (
        <div className="mb-5 rounded-2xl border border-amber-200 bg-amber-50 p-4 text-sm text-amber-900">
          Cần tạo và bật ít nhất một danh mục trước khi thêm sản phẩm.
        </div>
      )}

      {error && (
        <div role="alert" className="mb-5 rounded-2xl border border-rose-200 bg-rose-50 p-4 text-sm text-rose-700">
          {error}
        </div>
      )}

      <section className="rounded-2xl border border-line bg-white p-4 sm:p-5">
        <div className="mb-4 grid gap-3 md:grid-cols-[1fr_260px]">
          <label className="relative">
            <Search size={17} className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-400" />
            <input
              className={`${input} pl-11`}
              value={query}
              onChange={(event) => setQuery(event.target.value)}
              placeholder="Tìm theo tên hoặc mã sản phẩm"
            />
          </label>
          <select
            className={input}
            value={categoryFilter}
            onChange={(event) => setCategoryFilter(event.target.value)}
            aria-label="Lọc theo danh mục"
          >
            <option value="all">Tất cả danh mục</option>
            {categories.map((category) => (
              <option key={category.id} value={category.id}>{category.name}</option>
            ))}
          </select>
        </div>

        {products === null ? (
          <p className="px-3 py-10 text-center text-sm text-slate-500">Đang tải danh sách sản phẩm…</p>
        ) : filteredProducts.length === 0 ? (
          <div className="p-10 text-center">
            <Shirt size={28} className="mx-auto text-brand-700" />
            <h2 className="mt-4 font-display text-lg font-extrabold">Chưa có sản phẩm phù hợp</h2>
            <p className="mt-2 text-sm text-slate-500">Thử đổi bộ lọc hoặc tạo sản phẩm mới.</p>
            <button type="button" onClick={() => { setQuery(""); setCategoryFilter("all"); }} className="mt-4 text-sm font-bold text-brand-800">Xóa bộ lọc</button>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full min-w-[820px] text-left text-sm">
              <thead className="text-[10px] uppercase tracking-[.13em] text-slate-400">
                <tr>
                  <th className="px-3 py-3">Sản phẩm</th>
                  <th className="px-3 py-3">Danh mục</th>
                  <th className="px-3 py-3">Giá từ</th>
                  <th className="px-3 py-3">Biến thể / tồn kho</th>
                  <th className="px-3 py-3">Trạng thái</th>
                  <th className="px-3 py-3 text-right">Thao tác</th>
                </tr>
              </thead>
              <tbody>
                {filteredProducts.map((product) => {
                  const variants = product.variants || [];
                  const totalStock = variants.reduce((total, variant) => total + variant.stockQuantity, 0);
                  return (
                    <tr key={product.id} className="border-t border-line align-middle">
                      <td className="px-3 py-4">
                        <div className="flex items-center gap-3">
                          {product.imageUrl ? (
                            <img src={product.imageUrl} alt="" className="h-12 w-12 rounded-xl bg-slate-100 object-cover" />
                          ) : (
                            <div className="grid h-12 w-12 place-items-center rounded-xl bg-brand-50 text-brand-700"><Shirt size={19} /></div>
                          )}
                          <div className="min-w-0">
                            <p className="max-w-[260px] truncate font-bold text-ink">{product.name}</p>
                            <p className="mt-1 text-xs text-slate-400">Mã #{product.id}</p>
                          </div>
                        </div>
                      </td>
                      <td className="px-3 py-4 text-slate-600">
                        <span>{product.category || "Chưa gán danh mục"}</span>
                        {categories.find((category) => category.id === product.categoryId)?.active === false && (
                          <span className="mt-1 block text-[10px] font-bold text-amber-700">Danh mục đang tạm dừng</span>
                        )}
                      </td>
                      <td className="px-3 py-4 font-bold">{money(product.price)}</td>
                      <td className="px-3 py-4">
                        <p className="font-semibold">{variants.length} biến thể</p>
                        <p className="mt-1 text-xs text-slate-500">Tổng tồn: {totalStock}</p>
                      </td>
                      <td className="px-3 py-4">
                        <span className={`rounded-full px-2.5 py-1 text-xs font-bold ${product.status === "ACTIVE" ? "bg-emerald-50 text-emerald-700" : "bg-slate-100 text-slate-500"}`}>
                          {statusLabel[product.status] || product.status}
                        </span>
                      </td>
                      <td className="px-3 py-4">
                        <div className="flex justify-end gap-2">
                          <button
                            type="button"
                            onClick={() => setVariantsProductId(product.id)}
                            className="inline-flex items-center gap-1.5 rounded-lg border border-line px-3 py-2 text-xs font-bold text-slate-700 hover:border-brand-300 hover:text-brand-800"
                          >
                            <Boxes size={14} /> Biến thể
                          </button>
                          <button
                            type="button"
                            onClick={() => setEditingProduct(product)}
                            className="grid h-9 w-9 place-items-center rounded-lg border border-line text-slate-600 hover:border-brand-300 hover:text-brand-800"
                            title="Sửa sản phẩm"
                          >
                            <Edit3 size={15} />
                          </button>
                          {product.status === "ACTIVE" && (
                            <button
                              type="button"
                              onClick={() => archiveProduct(product)}
                              className="grid h-9 w-9 place-items-center rounded-lg border border-line text-slate-500 hover:border-rose-200 hover:text-rose-600"
                              title="Ẩn sản phẩm"
                            >
                              <Archive size={15} />
                            </button>
                          )}
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </section>

      {editingProduct !== undefined && (
        <ProductFormDialog
          key={editingProduct?.id ?? "new-product"}
          product={editingProduct}
          categories={categories}
          onClose={() => setEditingProduct(undefined)}
          onSaved={async () => {
            setEditingProduct(undefined);
            setToast?.("Đã lưu thông tin sản phẩm.");
            await load();
          }}
          onError={(message) => setToast?.(message)}
        />
      )}

      {variantsProduct && (
        <VariantManagerDialog
          key={variantsProduct.id}
          product={variantsProduct}
          onClose={() => setVariantsProductId(null)}
          onChanged={async () => {
            await load();
          }}
          onError={(message) => setToast?.(message)}
          onToast={(message) => setToast?.(message)}
        />
      )}
    </>
  );
}

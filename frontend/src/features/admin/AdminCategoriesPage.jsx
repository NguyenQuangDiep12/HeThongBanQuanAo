import { useEffect, useState } from "react";
import { FolderTree, Pencil, Plus, Power, Trash2 } from "lucide-react";
import { input, label, primary, secondary } from "../../shared/uiStyles.js";
import { Title } from "../../components/ui/PageTitle.jsx";
import {
  deleteAdminCategory,
  fetchAdminCategories,
  saveAdminCategory,
} from "../../services/adminService.js";

const blankCategory = { name: "", description: "", active: true };

export function AdminCategories({ setToast }) {
  const [categories, setCategories] = useState(null);
  const [editing, setEditing] = useState(undefined);
  const [form, setForm] = useState(blankCategory);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");

  const load = () => fetchAdminCategories()
    .then((rows) => {
      setCategories(rows);
      setError("");
    })
    .catch((loadError) => setError(loadError.message));

  useEffect(() => {
    let mounted = true;
    fetchAdminCategories()
      .then((rows) => { if (mounted) setCategories(rows); })
      .catch((loadError) => { if (mounted) setError(loadError.message); });
    return () => { mounted = false; };
  }, []);

  const open = (category = null) => {
    setEditing(category);
    setForm(category ? {
      name: category.name,
      description: category.description || "",
      active: category.active,
    } : { ...blankCategory });
  };

  const close = () => setEditing(undefined);

  const submit = async (event) => {
    event.preventDefault();
    setSaving(true);
    try {
      await saveAdminCategory(editing?.id ?? null, form);
      close();
      setToast?.(editing ? "Đã cập nhật danh mục." : "Đã tạo danh mục.");
      await load();
    } catch (saveError) {
      setToast?.(saveError.message);
    } finally {
      setSaving(false);
    }
  };

  const remove = async (category) => {
    if (category.productCount > 0) return;
    if (!window.confirm(`Xóa danh mục “${category.name}”?`)) return;
    try {
      await deleteAdminCategory(category.id);
      setToast?.("Đã xóa danh mục.");
      await load();
    } catch (deleteError) {
      setToast?.(deleteError.message);
    }
  };

  return (
    <>
      <Title
        eyebrow="Cấu trúc cửa hàng"
        title="Danh mục sản phẩm"
        description="Danh mục là cấp cha của sản phẩm. Hãy tạo danh mục trước, sau đó gán sản phẩm và cấu hình biến thể bên trong sản phẩm."
      >
        <button onClick={() => open()} className={primary}><Plus size={16} /> Tạo danh mục</button>
      </Title>

      {error && <div role="alert" className="mb-5 rounded-2xl border border-rose-200 bg-rose-50 p-4 text-sm text-rose-700">{error}</div>}
      <div className="mb-5 grid gap-3 sm:grid-cols-3">
        <Summary label="Tổng danh mục" value={categories?.length ?? "—"} />
        <Summary label="Đang hoạt động" value={categories?.filter((category) => category.active).length ?? "—"} />
        <Summary label="Sản phẩm đã phân loại" value={categories?.reduce((sum, category) => sum + category.productCount, 0) ?? "—"} />
      </div>

      <section className="overflow-hidden rounded-2xl border border-line bg-white">
        {categories === null ? (
          <p className="p-10 text-center text-sm text-slate-500">Đang tải danh mục…</p>
        ) : categories.length === 0 ? (
          <div className="p-10 text-center">
            <FolderTree size={28} className="mx-auto text-brand-700" />
            <h2 className="mt-4 font-display text-lg font-extrabold">Chưa có danh mục</h2>
            <p className="mt-2 text-sm text-slate-500">Tạo danh mục đầu tiên để bắt đầu phân loại sản phẩm.</p>
            <button type="button" onClick={() => open()} className={`${primary} mt-5`}><Plus size={15} /> Tạo danh mục</button>
          </div>
        ) : (
          <div className="divide-y divide-line">
            {categories.map((category) => (
              <article key={category.id} className="flex flex-col gap-4 p-4 sm:flex-row sm:items-center sm:justify-between sm:p-5">
                <div className="flex min-w-0 items-start gap-4">
                  <div className="grid h-12 w-12 shrink-0 place-items-center rounded-2xl bg-brand-50 text-brand-800"><FolderTree size={20} /></div>
                  <div className="min-w-0">
                    <div className="flex flex-wrap items-center gap-2">
                      <h2 className="font-bold text-ink">{category.name}</h2>
                      <span className={`rounded-full px-2.5 py-1 text-[10px] font-extrabold ${category.active ? "bg-emerald-50 text-emerald-700" : "bg-slate-100 text-slate-500"}`}>
                        {category.active ? "Đang hoạt động" : "Đã tạm dừng"}
                      </span>
                    </div>
                    <p className="mt-1 text-sm text-slate-500">{category.description || "Chưa có mô tả"}</p>
                    <p className="mt-2 text-xs font-semibold text-slate-400">{category.productCount} sản phẩm · Mã danh mục #{category.id}</p>
                  </div>
                </div>
                <div className="flex shrink-0 gap-2 sm:justify-end">
                  <button type="button" onClick={() => open(category)} className="inline-flex items-center gap-2 rounded-xl border border-line px-3 py-2 text-xs font-bold hover:border-brand-300 hover:text-brand-800"><Pencil size={14} /> Chỉnh sửa</button>
                  <button
                    type="button"
                    onClick={() => remove(category)}
                    disabled={category.productCount > 0}
                    title={category.productCount > 0 ? "Chuyển sản phẩm ra khỏi danh mục trước khi xóa" : "Xóa danh mục"}
                    className="grid h-9 w-9 place-items-center rounded-xl border border-line text-slate-500 hover:border-rose-200 hover:text-rose-600 disabled:cursor-not-allowed disabled:opacity-40"
                    aria-label="Xóa danh mục"
                  ><Trash2 size={15} /></button>
                </div>
              </article>
            ))}
          </div>
        )}
      </section>

      {editing !== undefined && (
        <div className="fixed inset-0 z-50 grid place-items-center bg-slate-950/50 p-4" onMouseDown={close}>
          <form onSubmit={submit} onMouseDown={(event) => event.stopPropagation()} className="w-full max-w-xl rounded-3xl bg-white p-6 shadow-2xl sm:p-7">
            <p className="text-[10px] font-extrabold uppercase tracking-[.16em] text-brand-700">Quản lý danh mục</p>
            <h2 className="mt-2 font-display text-2xl font-extrabold">{editing ? "Cập nhật danh mục" : "Tạo danh mục"}</h2>
            <p className="mt-1 text-sm text-slate-500">Danh mục giúp khách hàng lọc và khám phá sản phẩm.</p>

            <div className="mt-6">
              <label className={label}>Tên danh mục *</label>
              <input required autoFocus maxLength={80} className={input} placeholder="Ví dụ: Áo thun nam" value={form.name} onChange={(event) => setForm({ ...form, name: event.target.value })} />
            </div>
            <div className="mt-4">
              <label className={label}>Mô tả</label>
              <textarea maxLength={500} rows={3} className={input} placeholder="Mô tả ngắn về nhóm sản phẩm" value={form.description} onChange={(event) => setForm({ ...form, description: event.target.value })} />
            </div>
            {editing && (
              <label className="mt-4 flex cursor-pointer items-start gap-3 rounded-2xl border border-line p-4">
                <input type="checkbox" checked={form.active} onChange={(event) => setForm({ ...form, active: event.target.checked })} className="mt-1 accent-blue-700" />
                <span><span className="flex items-center gap-2 text-sm font-bold"><Power size={15} /> Cho phép bán trong danh mục này</span><span className="mt-1 block text-xs text-slate-500">Tắt danh mục sẽ ẩn sản phẩm thuộc danh mục khỏi trang cửa hàng.</span></span>
              </label>
            )}

            <footer className="mt-7 flex justify-end gap-3 border-t border-line pt-5">
              <button type="button" onClick={close} className={secondary}>Hủy</button>
              <button type="submit" disabled={saving} className={primary}>{saving ? "Đang lưu…" : "Lưu danh mục"}</button>
            </footer>
          </form>
        </div>
      )}
    </>
  );
}

function Summary({ label: title, value }) {
  return (
    <div className="rounded-2xl border border-line bg-white p-4">
      <p className="text-xs font-bold text-slate-500">{title}</p>
      <p className="mt-2 font-display text-2xl font-extrabold text-ink">{value}</p>
    </div>
  );
}

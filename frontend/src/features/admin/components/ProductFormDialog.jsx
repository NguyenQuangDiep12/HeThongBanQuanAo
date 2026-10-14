import { useState } from "react";
import { X } from "lucide-react";
import { input, label, primary, secondary } from "../../../shared/uiStyles.js";
import { saveAdminProduct } from "../../../services/adminService.js";

const initialForm = {
  name: "",
  categoryId: "",
  description: "",
  price: "",
  imageUrl: "",
  status: "ACTIVE",
};

export function ProductFormDialog({ product, categories, onClose, onSaved, onError }) {
  const [form, setForm] = useState(product ? {
    name: product.name,
    categoryId: String(product.categoryId || ""),
    description: product.description || "",
    price: String(product.price || ""),
    imageUrl: product.imageUrl || "",
    status: product.status || "ACTIVE",
  } : initialForm);
  const [saving, setSaving] = useState(false);
  const activeCategories = categories.filter((category) => category.active);

  const update = (field, value) => setForm((current) => ({ ...current, [field]: value }));

  const submit = async (event) => {
    event.preventDefault();
    setSaving(true);
    try {
      await saveAdminProduct(product?.id ?? null, {
        ...form,
        categoryId: Number(form.categoryId),
        price: Number(form.price),
      });
      await onSaved();
    } catch (error) {
      onError(error.message);
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 grid place-items-center bg-slate-950/50 p-4" onMouseDown={onClose}>
      <form
        onSubmit={submit}
        onMouseDown={(event) => event.stopPropagation()}
        className="max-h-[92vh] w-full max-w-2xl overflow-y-auto rounded-3xl bg-white p-5 shadow-2xl sm:p-7"
      >
        <header className="mb-6 flex items-start justify-between gap-4">
          <div>
            <p className="text-[10px] font-extrabold uppercase tracking-[.16em] text-brand-700">Danh mục → Sản phẩm → Biến thể</p>
            <h2 className="mt-2 font-display text-2xl font-extrabold">{product ? "Cập nhật sản phẩm" : "Tạo sản phẩm"}</h2>
            <p className="mt-1 text-sm text-slate-500">Chọn danh mục có sẵn. Size, màu và tồn kho được quản lý ở phần biến thể.</p>
          </div>
          <button type="button" onClick={onClose} aria-label="Đóng" className="rounded-xl p-2 text-slate-500 hover:bg-slate-100"><X size={20} /></button>
        </header>

        <div className="grid gap-4 sm:grid-cols-2">
          <div className="sm:col-span-2">
            <label className={label}>Tên sản phẩm *</label>
            <input required maxLength={180} autoFocus className={input} value={form.name} onChange={(event) => update("name", event.target.value)} />
          </div>
          <div>
            <label className={label}>Danh mục *</label>
            <select required className={input} value={form.categoryId} onChange={(event) => update("categoryId", event.target.value)}>
              <option value="">Chọn danh mục</option>
              {activeCategories.map((category) => <option key={category.id} value={category.id}>{category.name}</option>)}
            </select>
            {activeCategories.length === 0 && <p className="mt-1 text-xs text-rose-600">Chưa có danh mục đang hoạt động.</p>}
          </div>
          <div>
            <label className={label}>Giá sản phẩm (VND) *</label>
            <input required type="number" min="1" step="1" className={input} value={form.price} onChange={(event) => update("price", event.target.value)} />
          </div>
          <div>
            <label className={label}>Trạng thái</label>
            <select className={input} value={form.status} onChange={(event) => update("status", event.target.value)}>
              <option value="ACTIVE">Đang kinh doanh</option>
              <option value="INACTIVE">Đã ẩn</option>
            </select>
          </div>
          <div>
            <label className={label}>URL ảnh đại diện</label>
            <input type="text" maxLength={1000} className={input} placeholder="https://… hoặc /images/ao-thun.jpg" value={form.imageUrl} onChange={(event) => update("imageUrl", event.target.value)} />
          </div>
          {form.imageUrl && (
            <div className="sm:col-span-2">
              <img src={form.imageUrl} alt="Xem trước ảnh sản phẩm" className="h-36 w-36 rounded-2xl border border-line bg-slate-50 object-cover" />
            </div>
          )}
          <div className="sm:col-span-2">
            <label className={label}>Mô tả sản phẩm</label>
            <textarea rows={4} className={input} value={form.description} onChange={(event) => update("description", event.target.value)} />
          </div>
        </div>

        <footer className="mt-7 flex justify-end gap-3 border-t border-line pt-5">
          <button type="button" className={secondary} onClick={onClose}>Hủy</button>
          <button type="submit" disabled={saving || !activeCategories.length} className={primary}>{saving ? "Đang lưu…" : "Lưu sản phẩm"}</button>
        </footer>
      </form>
    </div>
  );
}

import { useState } from "react";
import { Plus, Trash2, X } from "lucide-react";
import { money } from "../../../api.js";
import { input, label, primary, secondary } from "../../../shared/uiStyles.js";
import {
  addAdminProductVariant,
  deleteAdminProductVariant,
  updateAdminProductVariant,
} from "../../../services/adminService.js";

const blankVariant = { size: "", color: "", stockQuantity: "0", price: "" };

export function VariantManagerDialog({ product, onClose, onChanged, onError, onToast }) {
  const [editingId, setEditingId] = useState(null);
  const [form, setForm] = useState(() => ({
    ...blankVariant,
    price: String(product.price),
  }));
  const [saving, setSaving] = useState(false);

  const beginEdit = (variant) => {
    setEditingId(variant.id);
    setForm({
      size: variant.size,
      color: variant.color,
      stockQuantity: String(variant.stockQuantity),
      price: String(variant.price),
    });
  };

  const reset = () => {
    setEditingId(null);
    setForm({ ...blankVariant, price: String(product.price) });
  };

  const submit = async (event) => {
    event.preventDefault();
    setSaving(true);
    const payload = {
      ...form,
      stockQuantity: Number(form.stockQuantity),
      price: Number(form.price),
    };
    try {
      if (editingId == null) {
        await addAdminProductVariant(product.id, payload);
        onToast("Đã thêm biến thể.");
      } else {
        await updateAdminProductVariant(product.id, editingId, payload);
        onToast("Đã cập nhật biến thể và tồn kho.");
      }
      reset();
      await onChanged();
    } catch (error) {
      onError(error.message);
    } finally {
      setSaving(false);
    }
  };

  const remove = async (variant) => {
    if (!window.confirm(`Xóa biến thể ${variant.size} / ${variant.color}?`)) return;
    try {
      await deleteAdminProductVariant(product.id, variant.id);
      onToast("Đã xóa biến thể.");
      if (editingId === variant.id) reset();
      await onChanged();
    } catch (error) {
      onError(error.message);
    }
  };

  return (
    <div className="fixed inset-0 z-50 grid place-items-center bg-slate-950/50 p-4" onMouseDown={onClose}>
      <div onMouseDown={(event) => event.stopPropagation()} className="max-h-[92vh] w-full max-w-3xl overflow-y-auto rounded-3xl bg-white p-5 shadow-2xl sm:p-7">
        <header className="flex items-start justify-between gap-4">
          <div>
            <p className="text-[10px] font-extrabold uppercase tracking-[.16em] text-brand-700">Biến thể sản phẩm</p>
            <h2 className="mt-2 font-display text-2xl font-extrabold">{product.name}</h2>
            <p className="mt-1 text-sm text-slate-500">Quản lý size, màu sắc, giá và tồn kho theo từng SKU.</p>
          </div>
          <button type="button" onClick={onClose} aria-label="Đóng" className="rounded-xl p-2 text-slate-500 hover:bg-slate-100"><X size={20} /></button>
        </header>

        <div className="mt-6 overflow-x-auto rounded-2xl border border-line">
          <table className="w-full min-w-[540px] text-left text-sm">
            <thead className="bg-slate-50 text-[10px] uppercase tracking-[.12em] text-slate-500">
              <tr><th className="p-3">Size</th><th className="p-3">Màu sắc</th><th className="p-3">Giá</th><th className="p-3">Tồn</th><th className="p-3 text-right">Thao tác</th></tr>
            </thead>
            <tbody>
              {(product.variants || []).map((variant) => (
                <tr key={variant.id} className="border-t border-line">
                  <td className="p-3 font-bold">{variant.size}</td>
                  <td className="p-3">{variant.color}</td>
                  <td className="p-3">{money(variant.price)}</td>
                  <td className="p-3"><span className={variant.stockQuantity > 0 ? "font-bold text-emerald-700" : "font-bold text-rose-600"}>{variant.stockQuantity}</span></td>
                  <td className="p-3">
                    <div className="flex justify-end gap-2">
                      <button type="button" onClick={() => beginEdit(variant)} className="rounded-lg border border-line px-3 py-1.5 text-xs font-bold hover:border-brand-300">Sửa</button>
                      <button type="button" onClick={() => remove(variant)} aria-label="Xóa biến thể" className="grid h-8 w-8 place-items-center rounded-lg border border-line text-slate-500 hover:border-rose-200 hover:text-rose-600"><Trash2 size={14} /></button>
                    </div>
                  </td>
                </tr>
              ))}
              {!product.variants?.length && <tr><td colSpan="5" className="p-6 text-center text-sm text-slate-500">Sản phẩm chưa có biến thể. Hãy thêm size và màu để khách đặt hàng.</td></tr>}
            </tbody>
          </table>
        </div>

        <form onSubmit={submit} className="mt-6 rounded-2xl bg-slate-50 p-4 sm:p-5">
          <div className="mb-4 flex items-center justify-between gap-3">
            <h3 className="font-bold">{editingId == null ? "Thêm biến thể" : "Chỉnh sửa biến thể"}</h3>
            {editingId != null && <button type="button" onClick={reset} className="text-xs font-bold text-brand-700">Hủy sửa</button>}
          </div>
          <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
            <div><label className={label}>Kích cỡ *</label><input required maxLength={40} className={input} placeholder="S, M, L, XL" value={form.size} onChange={(event) => setForm({ ...form, size: event.target.value })} /></div>
            <div><label className={label}>Màu sắc *</label><input required maxLength={60} className={input} placeholder="Đen, Trắng…" value={form.color} onChange={(event) => setForm({ ...form, color: event.target.value })} /></div>
            <div><label className={label}>Giá bán *</label><input required type="number" min="1" step="1" className={input} value={form.price} onChange={(event) => setForm({ ...form, price: event.target.value })} /></div>
            <div><label className={label}>Số lượng tồn *</label><input required type="number" min="0" step="1" className={input} value={form.stockQuantity} onChange={(event) => setForm({ ...form, stockQuantity: event.target.value })} /></div>
          </div>
          <div className="mt-4 flex justify-end">
            <button type="submit" disabled={saving} className={primary}><Plus size={15} />{saving ? "Đang lưu…" : editingId == null ? "Thêm biến thể" : "Lưu thay đổi"}</button>
          </div>
        </form>

        <div className="mt-5 flex justify-end"><button type="button" onClick={onClose} className={secondary}>Đóng</button></div>
      </div>
    </div>
  );
}

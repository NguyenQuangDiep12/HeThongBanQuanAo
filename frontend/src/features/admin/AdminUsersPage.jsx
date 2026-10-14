import { useEffect, useState } from "react";
import { ShieldCheck, UserRound, UserX } from "lucide-react";
import { Title } from "../../components/ui/PageTitle.jsx";
import { date } from "../../shared/format.js";
import { fetchAdminUsers, setAdminUserActive } from "../../services/adminService.js";

export function AdminUsers({ setToast }) {
  const [users, setUsers] = useState([]), [error, setError] = useState(""), [busyId, setBusyId] = useState(null);
  const load = () => fetchAdminUsers().then(setUsers).catch((e) => setError(e.message));
  useEffect(() => { load(); }, []);
  const toggle = async (user) => {
    setBusyId(user.id);
    try {
      const updated = await setAdminUserActive(user.id, !user.active);
      setUsers((rows) => rows.map((row) => row.id === user.id ? updated : row));
      setToast(updated.active ? "Đã kích hoạt tài khoản" : "Đã vô hiệu hóa tài khoản");
    } catch (e) { setToast(e.message); }
    finally { setBusyId(null); }
  };
  return <>
    <Title eyebrow="Quản trị" title="Người dùng" description="Xem tài khoản khách hàng và quản lý trạng thái truy cập." />
    {error && <p role="alert" className="mb-4 rounded-xl bg-rose-50 p-4 text-sm text-rose-700">{error}</p>}
    <div className="overflow-x-auto rounded-2xl border border-slate-200 bg-white">
      <table className="w-full min-w-[680px] text-left text-sm">
        <thead className="bg-slate-50 text-xs text-slate-500"><tr>{["Người dùng", "Email", "Vai trò", "Ngày tạo", "Trạng thái", "Thao tác"].map((x) => <th key={x} className="px-4 py-3">{x}</th>)}</tr></thead>
        <tbody className="divide-y divide-slate-100">{users.map((user) => <tr key={user.id}>
          <td className="px-4 py-3 font-bold text-ink"><UserRound size={15} className="mr-2 inline text-slate-400" />{user.fullName}</td>
          <td className="px-4 py-3 text-slate-600">{user.email}<div className="text-xs text-slate-400">{user.phone || "Chưa có số điện thoại"}</div></td>
          <td className="px-4 py-3">{user.role === "ADMIN" ? <span className="text-brand-700"><ShieldCheck size={14} className="mr-1 inline" />Admin</span> : "Khách hàng"}</td>
          <td className="px-4 py-3 text-slate-500">{date(user.createdAt)}</td>
          <td className="px-4 py-3"><span className={user.active ? "text-emerald-700" : "text-rose-700"}>{user.active ? "Đang hoạt động" : "Đã khóa"}</span></td>
          <td className="px-4 py-3">{user.role === "USER" && <button disabled={busyId === user.id} onClick={() => toggle(user)} className="inline-flex items-center gap-1 rounded-lg border px-3 py-2 text-xs font-bold disabled:opacity-50">{user.active ? <><UserX size={14} /> Khóa</> : "Mở khóa"}</button>}</td>
        </tr>)}</tbody>
      </table>
      {!users.length && !error && <p className="p-8 text-center text-sm text-slate-400">Chưa có tài khoản.</p>}
    </div>
  </>;
}

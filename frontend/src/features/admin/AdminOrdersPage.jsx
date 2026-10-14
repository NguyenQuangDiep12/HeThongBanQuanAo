import { useEffect, useState } from 'react';
import { money } from '../../api';
import { cx } from '../../shared/uiStyles.js';
import { date } from '../../shared/format.js';
import { Pill } from '../../components/ui/StatusPill.jsx';
import { Title } from '../../components/ui/PageTitle.jsx';
import { fetchAdminOrders, updateAdminOrderStatus } from '../../services/adminService.js';

const filters = [
  ['ALL', 'Tất cả'],
  ['PENDING', 'Chờ xác nhận'],
  ['CONFIRMED', 'Đã xác nhận'],
  ['SHIPPING', 'Đang giao'],
  ['DELIVERED', 'Đã giao'],
  ['COMPLETED', 'Đã nhận hàng'],
  ['CANCELLED', 'Đã hủy']
];

function actionsFor(order) {
  if (order.status === 'PENDING') return [['confirm', 'Xác nhận'], ['cancel', 'Hủy']];
  if (order.status === 'CONFIRMED') return [['start-delivery', 'Bắt đầu giao']];
  if (order.status === 'SHIPPING') return [['mark-delivered', 'Xác nhận đã giao']];
  return [];
}

export function AdminOrders({ setToast }) {
  const [items, setItems] = useState(null);
  const [filter, setFilter] = useState('ALL');
  const [err, setErr] = useState('');
  const [busyId, setBusyId] = useState(null);

  const load = () => fetchAdminOrders().then(setItems).catch(e => setErr(e.message));
  useEffect(() => { load(); }, []);

  const act = async (order, action) => {
    if (busyId != null) return;
    if (action === 'cancel' && !window.confirm('Hủy đơn và hoàn lại tồn kho?')) return;
    setBusyId(order.id);
    try {
      await updateAdminOrderStatus(order.id, action);
      const messages = {
        confirm: 'Đã xác nhận đơn hàng.',
        cancel: 'Đã hủy đơn và hoàn kho.',
        'start-delivery': 'Đã chuyển đơn sang đang giao.',
        'mark-delivered': 'Đã cập nhật đơn đã giao.'
      };
      setToast(messages[action]);
      await load();
    } catch (e) {
      setToast(e.message);
    } finally {
      setBusyId(null);
    }
  };

  const rows = (items || []).filter(order => filter === 'ALL' || order.status === filter);
  return (
    <>
      <Title eyebrow="Vận hành cửa hàng" title="Quản lý đơn hàng" description="Xác nhận đơn, cập nhật giao hàng và theo dõi trạng thái." />
      <div className="mb-4 flex gap-2 overflow-auto">
        {filters.map(([key, title]) => (
          <button key={key} onClick={() => setFilter(key)} className={cx(
            'shrink-0 rounded-full px-4 py-2 text-xs font-bold',
            filter === key ? 'bg-brand-800 text-white' : 'bg-slate-100'
          )}>{title}</button>
        ))}
      </div>
      {err && <p className="mb-3 text-sm text-rose-600">{err}</p>}
      <div className="overflow-auto rounded-2xl border border-line">
        <table className="w-full min-w-[900px] text-left text-xs">
          <thead className="bg-slate-50 text-[10px] uppercase text-slate-400">
            <tr>{['Đơn hàng', 'Địa chỉ / điện thoại', 'Sản phẩm', 'Tổng', 'Trạng thái', 'Thao tác'].map(title => <th key={title} className="p-3">{title}</th>)}</tr>
          </thead>
          <tbody>
            {rows.map(order => (
              <tr key={order.id} className="border-t border-line">
                <td className="p-3 font-bold">#BT-{order.id}<small className="block text-slate-400">{date(order.createdAt)}</small></td>
                <td className="max-w-40 p-3">{order.shippingAddress}<small className="block text-slate-400">{order.phone}</small></td>
                <td className="max-w-40 p-3">{order.items?.map(item => item.productName + ' ×' + item.quantity).join(', ')}</td>
                <td className="p-3 font-bold">{money(order.totalAmount)}</td>
                <td className="p-3"><Pill status={order.status} /></td>
                <td className="p-3">
                  <div className="flex gap-2">
                    {actionsFor(order).map(([action, title]) => (
                      <button
                        key={action}
                        disabled={busyId != null}
                        onClick={() => act(order, action)}
                        className={cx(
                          'whitespace-nowrap rounded-lg px-3 py-2 font-bold disabled:opacity-50',
                          action === 'cancel' ? 'border text-rose-600' : 'bg-brand-800 text-white'
                        )}
                      >{busyId === order.id ? 'Đang xử lý…' : title}</button>
                    ))}
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {items && rows.length === 0 && <p className="p-8 text-center text-sm text-slate-400">Không có đơn hàng.</p>}
      </div>
    </>
  );
}
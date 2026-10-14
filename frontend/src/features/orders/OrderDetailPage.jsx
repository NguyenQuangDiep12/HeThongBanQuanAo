import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { ArrowLeft, CheckCheck, X } from 'lucide-react';
import { money } from '../../api';
import { cx, primary, secondary, label } from '../../shared/uiStyles.js';
import { date } from '../../shared/format.js';
import { Pill } from '../../components/ui/StatusPill.jsx';
import { Title } from '../../components/ui/PageTitle.jsx';
import { Empty } from '../../components/ui/EmptyState.jsx';
import { OrderTimeline } from '../../components/orders/OrderTimeline.jsx';
import { cancelMyOrder, confirmOrderReceipt, fetchMyOrder } from '../../services/orderService.js';

export function OrderDetail({ setToast }) {
  const { id } = useParams();
  const [order, setOrder] = useState(null);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    let active = true;
    fetchMyOrder(id)
      .then(value => { if (active) setOrder(value); })
      .catch(e => { if (active) setError(e.message); });
    return () => { active = false; };
  }, [id]);

  const performAction = async (action) => {
    if (busy) return;
    if (action === 'cancel' && !window.confirm('Bạn muốn hủy đơn hàng này? Tồn kho sẽ được hoàn lại.')) return;
    setBusy(true);
    try {
      const updated = action === 'cancel'
        ? await cancelMyOrder(id)
        : await confirmOrderReceipt(id);
      setOrder(updated);
      setToast?.(action === 'cancel' ? 'Đơn hàng đã được hủy.' : 'Đã xác nhận nhận hàng.');
    } catch (e) {
      setToast?.(e.message);
    } finally {
      setBusy(false);
    }
  };

  if (!order) {
    return <main className="shell py-14">{error
      ? <Empty title="Không tìm thấy đơn hàng" body={error} to="/don-hang" action="Đơn hàng của tôi" />
      : <p>Đang tải…</p>}</main>;
  }

  return (
    <main className="shell py-12">
      <Title
        eyebrow="Chi tiết đơn hàng"
        title={'Đơn #BT-' + String(order.id).padStart(5, '0')}
        description={'Đặt ngày ' + date(order.createdAt)}
      >
        <Pill status={order.status} />
      </Title>

      <div className="mb-6 flex flex-wrap gap-3">
        {order.status === 'PENDING' && (
          <button disabled={busy} onClick={() => performAction('cancel')} className={cx(secondary, 'text-rose-700')}>
            <X size={16} /> {busy ? 'Đang xử lý…' : 'Hủy đơn hàng'}
          </button>
        )}
        {order.status === 'DELIVERED' && (
          <button disabled={busy} onClick={() => performAction('confirm-receipt')} className={primary}>
            <CheckCheck size={16} /> {busy ? 'Đang xử lý…' : 'Xác nhận đã nhận hàng'}
          </button>
        )}
      </div>

      {order.status === 'CANCELLED' && (
        <p className="mb-6 rounded-xl bg-rose-50 p-4 text-sm text-rose-700">Đơn hàng đã bị hủy. Tồn kho đã được hoàn lại.</p>
      )}

      <div className="mb-6">
        <OrderTimeline order={order} />
      </div>

      <div className="grid gap-6 lg:grid-cols-[1fr_330px]">
        <section className="rounded-2xl border border-line p-6">
          <h2 className="font-display font-extrabold">Sản phẩm trong đơn</h2>
          {order.items?.map(item => (
            <div key={item.id} className="flex items-center justify-between border-b border-line py-4 text-sm">
              <div>
                <b>{item.productName}</b>
                <p className="mt-1 text-xs text-slate-400">{item.color} · {item.size} · SL {item.quantity}</p>
              </div>
              <b>{money(item.subTotal)}</b>
            </div>
          ))}
        </section>
        <aside className="rounded-2xl border border-line p-6">
          <h2 className="font-display font-extrabold">Giao hàng & thanh toán</h2>
          <p className={label + ' mt-5'}>Địa chỉ</p>
          <p className="text-sm leading-6">{order.shippingAddress}</p>
          <p className={label + ' mt-4'}>Điện thoại</p>
          <p className="text-sm">{order.phone}</p>
          <p className={label + ' mt-4'}>Phương thức</p>
          <p className="text-sm">{order.payment?.paymentMethod === 'COD' ? 'Thanh toán khi nhận' : 'Chuyển khoản'}</p>
          <p className="mt-5 border-t border-line pt-4 font-extrabold">
            Tổng cộng <b className="float-right text-brand-800">{money(order.totalAmount)}</b>
          </p>
        </aside>
      </div>
      <Link to="/don-hang" className={secondary + ' mt-6'}><ArrowLeft size={15} /> Đơn hàng của tôi</Link>
    </main>
  );
}
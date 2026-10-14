import { Check } from 'lucide-react';
import { dateTime } from '../../shared/format.js';

export function OrderTimeline({ order }) {
  const events = [
    ['Đơn hàng được tạo', order.createdAt],
    ['Đơn hàng được xác nhận', order.confirmedAt],
    ['Bắt đầu giao hàng', order.shippingAt],
    ['Đơn hàng được đánh dấu đã giao', order.deliveredAt],
    ['Bạn xác nhận đã nhận hàng', order.customerConfirmedAt],
    ['Đơn hàng bị hủy', order.cancelledAt]
  ].filter(([, timestamp]) => Boolean(timestamp));

  return (
    <section className="rounded-2xl border border-line p-6">
      <h2 className="font-display font-extrabold">Lịch sử đơn hàng</h2>
      <ol className="mt-5 space-y-5">
        {events.map(([title, timestamp], index) => (
          <li key={title} className="flex gap-3">
            <span className="grid h-7 w-7 shrink-0 place-items-center rounded-full bg-brand-50 text-brand-700">
              <Check size={15} />
            </span>
            <div>
              <p className="text-sm font-bold">{title}</p>
              <time className="mt-1 block text-xs text-slate-500" dateTime={timestamp}>{dateTime(timestamp)}</time>
            </div>
          </li>
        ))}
      </ol>
      {order.status !== 'CANCELLED' && order.status !== 'COMPLETED' && (
        <p className="mt-5 border-t border-line pt-4 text-xs text-slate-500">
          Các mốc tiếp theo sẽ xuất hiện khi trạng thái đơn hàng được cập nhật.
        </p>
      )}
    </section>
  );
}
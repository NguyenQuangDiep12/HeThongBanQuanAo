import { cx } from '../../shared/uiStyles.js';
import { orderText } from '../../shared/orderStatus.js';

const statusStyles = {
  PENDING: 'bg-amber-50 text-amber-700',
  CONFIRMED: 'bg-blue-50 text-blue-700',
  SHIPPING: 'bg-indigo-50 text-indigo-700',
  DELIVERED: 'bg-cyan-50 text-cyan-800',
  COMPLETED: 'bg-emerald-50 text-emerald-700',
  CANCELLED: 'bg-rose-50 text-rose-700'
};

export function Pill({ status }) {
  return (
    <span className={cx('rounded-full px-3 py-1 text-xs font-bold', statusStyles[status] || 'bg-slate-100 text-slate-600')}>
      {orderText[status] || status}
    </span>
  );
}
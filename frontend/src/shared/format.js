export const date = x => x ? new Date(x).toLocaleDateString('vi-VN', {
  day: '2-digit',
  month: 'short',
  year: 'numeric'
}) : '—';

export const dateTime = x => x ? new Date(x).toLocaleString('vi-VN', {
  day: '2-digit',
  month: 'short',
  year: 'numeric',
  hour: '2-digit',
  minute: '2-digit'
}) : '—';
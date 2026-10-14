import { useEffect, useState } from "react";
import { Bell } from "lucide-react";
import { cx } from "../../shared/uiStyles.js";
import { date } from "../../shared/format.js";
import { Title } from "../../components/ui/PageTitle.jsx";
import { Empty } from "../../components/ui/EmptyState.jsx";
import {
  fetchNotifications,
  markNotificationRead,
} from "../../services/notificationService.js";

export function Notifications({ setToast }) {
  const [items, setItems] = useState(null),
    [error, setError] = useState("");
  const load = () =>
    fetchNotifications()
      .then(setItems)
      .catch((e) => setError(e.message));
  useEffect(() => {
    load();
  }, []);
  return (
    <main className="shell py-12">
      <Title
        eyebrow="Cập nhật cửa hàng"
        title="Thông báo"
        description="Thông tin mới nhất về đơn hàng."
      />
      {error ? (
        <Empty
          Icon={Bell}
          title="Không tải được thông báo"
          body={error}
          to="/dang-nhap"
          action="Đăng nhập"
        />
      ) : !items ? (
        <p>Đang tải…</p>
      ) : items.length ? (
        <div className="divide-y divide-line rounded-2xl border border-line">
          {items.map((n) => (
            <div
              key={n.id}
              className={cx("flex gap-4 p-5", !n.read && "bg-brand-50/50")}
            >
              <Bell className="text-brand-700" />
              <div className="flex-1">
                <b>{n.title}</b>
                <p className="mt-1 text-sm text-slate-500">{n.message}</p>
                <small className="text-slate-400">{date(n.createdAt)}</small>
              </div>
              {!n.read && (
                <button
                  onClick={async () => {
                    try {
                      await markNotificationRead(n.id);
                      load();
                    } catch (e) {
                      setToast(e.message);
                    }
                  }}
                  className="text-xs font-bold text-brand-700"
                >
                  Đã đọc
                </button>
              )}
            </div>
          ))}
        </div>
      ) : (
        <Empty
          Icon={Bell}
          title="Chưa có thông báo"
          body="Cập nhật đơn hàng sẽ xuất hiện tại đây."
        />
      )}
    </main>
  );
}

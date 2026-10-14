import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { Package } from "lucide-react";
import { money } from "../../api";
import { date } from "../../shared/format.js";
import { Pill } from "../../components/ui/StatusPill.jsx";
import { Title } from "../../components/ui/PageTitle.jsx";
import { Empty } from "../../components/ui/EmptyState.jsx";
import { fetchMyOrders } from "../../services/orderService.js";

export function Orders() {
  const [list, setList] = useState(null),
    [error, setError] = useState("");
  useEffect(() => {
    fetchMyOrders()
      .then(setList)
      .catch((e) => setError(e.message));
  }, []);
  return (
    <main className="shell py-12">
      <Title
        eyebrow="Tài khoản của bạn"
        title="Đơn hàng của tôi"
        description="Theo dõi trạng thái xử lý đơn hàng."
      />
      {error ? (
        <Empty
          Icon={Package}
          title="Không tải được đơn hàng"
          body={error}
          to="/dang-nhap"
          action="Đăng nhập"
        />
      ) : !list ? (
        <p>Đang tải…</p>
      ) : list.length ? (
        <div className="grid gap-4">
          {list.map((o) => (
            <Link
              to={"/don-hang/" + o.id}
              key={o.id}
              className="rounded-2xl border border-line p-5 hover:border-brand-200"
            >
              <div className="flex justify-between">
                <div>
                  <b className="font-display">
                    #BT-{String(o.id).padStart(5, "0")}
                  </b>
                  <p className="mt-1 text-xs text-slate-400">
                    {date(o.createdAt)} · {o.items?.length || 0} sản phẩm
                  </p>
                </div>
                <div className="text-right">
                  <Pill status={o.status} />
                  <p className="mt-2 font-bold">{money(o.totalAmount)}</p>
                </div>
              </div>
              <p className="mt-4 border-t border-line pt-3 text-xs text-slate-500">
                {o.items?.map((i) => i.productName).join(", ")}{" "}
                <b className="float-right text-brand-700">Chi tiết →</b>
              </p>
            </Link>
          ))}
        </div>
      ) : (
        <Empty
          Icon={Package}
          title="Chưa có đơn hàng"
          body="Đơn hàng sẽ hiển thị ở đây sau khi bạn đặt mua."
        />
      )}
    </main>
  );
}

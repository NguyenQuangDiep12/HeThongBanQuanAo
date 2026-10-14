import { Link } from 'react-router-dom';

export function Footer() {
  return (
    <footer className="mt-20 bg-[#101f38] text-white">
      <div className="shell grid gap-8 py-12 sm:grid-cols-2 lg:grid-cols-4">
        <div>
          <b className="font-display text-xl">BLUE THREAD.</b>

          <p className="mt-3 text-sm leading-6 text-slate-400">
            Những món đồ dễ mặc, dễ phối và đồng hành mỗi ngày.
          </p>
        </div>

        <div>
          <b className="text-xs uppercase tracking-widest">Khám phá</b>

          <div className="mt-4 grid gap-3 text-sm text-slate-400">
            <Link to="/san-pham">Tất cả sản phẩm</Link>
            <Link to="/san-pham?sort=newest">Bộ sưu tập mới</Link>
          </div>
        </div>

        <div>
          <b className="text-xs uppercase tracking-widest">Hỗ trợ</b>

          <div className="mt-4 grid gap-3 text-sm text-slate-400">
            <span>Đổi trả trong 7 ngày</span>
            <span>Thanh toán khi nhận hàng</span>
          </div>
        </div>

        <div>
          <b className="text-xs uppercase tracking-widest">BLUE THREAD</b>

          <p className="mt-4 text-sm leading-6 text-slate-400">
            Thời trang tinh giản cho nhịp sống hiện đại.
          </p>
        </div>
      </div>

      <div className="border-t border-white/10 py-4 text-center text-xs text-slate-500">
        © 2026 BLUE THREAD · Đặt hàng thuận tiện, theo dõi tiến trình đơn hàng
      </div>
    </footer>
  );
}

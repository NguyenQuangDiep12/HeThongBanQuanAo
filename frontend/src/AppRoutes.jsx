import { Link, Route, Routes } from 'react-router-dom';
import { LockKeyhole, ShieldCheck } from 'lucide-react';
import { primary } from './shared/uiStyles.js';
import { Empty } from './components/ui/EmptyState.jsx';
import { AdminLayout } from './features/admin/AdminLayout.jsx';
import { AdminHome } from './features/admin/AdminOverviewPage.jsx';
import { AdminProducts } from './features/admin/AdminProductsPage.jsx';
import { AdminCategories } from './features/admin/AdminCategoriesPage.jsx';
import { AdminOrders } from './features/admin/AdminOrdersPage.jsx';
import { Home } from './features/home/HomePage.jsx';
import { Catalog } from './features/catalog/CatalogPage.jsx';
import { Detail } from './features/products/ProductDetailPage.jsx';
import { Auth } from './features/auth/AuthPage.jsx';
import { Cart } from './features/cart/CartPage.jsx';
import { Checkout } from './features/cart/CheckoutPage.jsx';
import { Orders } from './features/orders/OrdersPage.jsx';
import { OrderDetail } from './features/orders/OrderDetailPage.jsx';
import { Notifications } from './features/notifications/NotificationsPage.jsx';
import { Account } from './features/account/AccountPage.jsx';

function AdminAccess({ user, children, to = '/' }) {
  if (!user) {
    return (
      <Empty
        Icon={LockKeyhole}
        title="Đăng nhập quản trị"
        body="Đăng nhập bằng tài khoản ADMIN để mở khu vực quản trị."
        to={'/dang-nhap?redirect=' + encodeURIComponent(to)}
        action="Đăng nhập"
      />
    );
  }

  if (user.role !== 'ADMIN') {
    return (
      <Empty
        Icon={ShieldCheck}
        title="Bạn chưa có quyền truy cập"
        body="Khu vực dành cho tài khoản ADMIN."
        to={to}
        action={to === '/' ? 'Về trang chủ' : undefined}
      />
    );
  }

  return <AdminLayout>{children}</AdminLayout>;
}

function CustomerAccess({ user, children, to }) {
  if (!user) {
    return (
      <Empty
        Icon={LockKeyhole}
        title="Đăng nhập để tiếp tục"
        body="Đăng nhập bằng tài khoản khách hàng để dùng giỏ hàng, đặt hàng và theo dõi đơn."
        to={'/dang-nhap?redirect=' + encodeURIComponent(to)}
        action="Đăng nhập"
      />
    );
  }

  if (user.role !== 'USER') {
    return (
      <Empty
        Icon={ShieldCheck}
        title="Khu vực này dành cho khách hàng"
        body="Bạn đang dùng tài khoản quản trị. Hãy mở trang quản trị để tiếp tục."
        to="/admin"
        action="Về trang quản trị"
      />
    );
  }

  return children;
}

export function AppRoutes({ user, setUser, setToast, refreshCart, logout }) {
  return (
    <Routes>
      <Route path="/" element={<Home />} />
      <Route path="/san-pham" element={<Catalog />} />
      <Route path="/san-pham/:id" element={<Detail user={user} setToast={setToast} onCart={refreshCart} />} />
      <Route path="/dang-nhap" element={<Auth setToast={setToast} onLogin={setUser} />} />
      <Route path="/gio-hang" element={<CustomerAccess user={user} to="/gio-hang"><Cart setToast={setToast} onChange={refreshCart} /></CustomerAccess>} />
      <Route path="/thanh-toan" element={<CustomerAccess user={user} to="/thanh-toan"><Checkout setToast={setToast} onChange={refreshCart} /></CustomerAccess>} />
      <Route path="/don-hang" element={<CustomerAccess user={user} to="/don-hang"><Orders /></CustomerAccess>} />
      <Route path="/don-hang/:id" element={<CustomerAccess user={user} to={window.location.pathname}><OrderDetail setToast={setToast} /></CustomerAccess>} />
      <Route path="/thong-bao" element={<CustomerAccess user={user} to="/thong-bao"><Notifications setToast={setToast} /></CustomerAccess>} />
      <Route
        path="/tai-khoan"
        element={<CustomerAccess user={user} to="/tai-khoan"><Account user={user} logout={logout} /></CustomerAccess>}
      />
      <Route path="/admin" element={<AdminAccess user={user}><AdminHome /></AdminAccess>} />
      <Route path="/admin/danh-muc" element={<AdminAccess user={user}><AdminCategories setToast={setToast} /></AdminAccess>} />
      <Route path="/admin/san-pham" element={<AdminAccess user={user}><AdminProducts setToast={setToast} /></AdminAccess>} />
      <Route path="/admin/don-hang" element={<AdminAccess user={user}><AdminOrders setToast={setToast} /></AdminAccess>} />
      <Route
        path="*"
        element={
          <main className="shell py-24 text-center">
            <h1 className="font-display text-5xl font-extrabold text-brand-800">404</h1>
            <p className="mt-3">Không tìm thấy trang.</p>
            <Link to="/" className={`${primary} mt-5`}>Trang chủ</Link>
          </main>
        }
      />
    </Routes>
  );
}


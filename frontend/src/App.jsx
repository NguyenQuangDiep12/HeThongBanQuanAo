import { useEffect, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { clearSession, getToken, getUser } from './api';
import { useToast } from './hooks/useToast.js';
import { Toast } from './components/ui/Toast.jsx';
import { Header } from './components/layout/Header.jsx';
import { Footer } from './components/layout/Footer.jsx';
import { AppRoutes } from './AppRoutes.jsx';
import { fetchCart } from './services/cartService.js';

function App() {
  const [user, setUser] = useState(getUser);
  const [cartCount, setCartCount] = useState(0);
  const { toast, setToast } = useToast();
  const location = useLocation();
  const navigate = useNavigate();

  const refreshCart = () => {
    if (!getToken() || user?.role !== "USER") {
      setCartCount(0);
      return;
    }

    fetchCart()
      .then(cart => setCartCount(cart?.items?.reduce((count, item) => count + item.quantity, 0) || 0))
      .catch(() => setCartCount(0));
  };

  useEffect(() => {
    refreshCart();
  }, [user]);

  useEffect(() => {
    const handleSessionExpired = () => {
      setUser(null);
      setCartCount(0);
    };

    window.addEventListener("shopnest:session-expired", handleSessionExpired);

    return () => {
      window.removeEventListener(
        "shopnest:session-expired",
        handleSessionExpired,
      );
    };
  }, []);

  useEffect(() => {
    window.scrollTo(0, 0);
  }, [location.pathname]);

  const logout = () => {
    clearSession();
    setUser(null);
    setCartCount(0);
    navigate('/');
  };

  return (
    <div className="min-h-screen">
      <Header user={user} count={cartCount} logout={logout} />
      <AppRoutes user={user} setUser={setUser} setToast={setToast} refreshCart={refreshCart} logout={logout} />
      <Footer />
      <Toast message={toast} />
    </div>
  );
}

export default App;

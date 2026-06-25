import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useCart } from '../context/CartContext';

export default function Nav() {
  const { username, isAdmin, logout } = useAuth();
  const { count } = useCart();
  const navigate = useNavigate();

  return (
    <nav className="nav">
      <div className="nav-inner">
        <Link to="/" className="brand">
          <span className="brand-mark">V</span> Vyapaar
        </Link>
        <div className="nav-links">
          <Link to="/">Shop</Link>
          {username && <Link to="/orders">My Orders</Link>}
          <Link to="/cart" className="cart-link">
            🛒
            {count > 0 && <span className="cart-badge">{count}</span>}
          </Link>
          {username ? (
            <div className="nav-user-box">
              <span className="nav-user">
                {username}{isAdmin && <span className="role-tag">ADMIN</span>}
              </span>
              <button className="btn btn-ghost sm" onClick={() => { logout(); navigate('/login'); }}>
                Logout
              </button>
            </div>
          ) : (
            <Link to="/login" className="btn btn-primary sm">Login</Link>
          )}
        </div>
      </div>
    </nav>
  );
}

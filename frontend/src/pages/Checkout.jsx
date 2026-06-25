import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import api from '../api';
import { useCart } from '../context/CartContext';
import { useAuth } from '../context/AuthContext';
import { formatINR } from '../format';

export default function Checkout() {
  const { lines, total, clear } = useCart();
  const { username } = useAuth();
  const navigate = useNavigate();
  const [address, setAddress] = useState('221B Baker Street, Mumbai 400001');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);

  if (!username) {
    return (
      <div className="card pad empty">
        <h2>Please log in to check out</h2>
        <Link to="/login" className="btn btn-primary">Log in</Link>
      </div>
    );
  }
  if (lines.length === 0) {
    return <div className="card pad empty"><h2>Your cart is empty</h2><Link to="/" className="btn btn-primary">Shop now</Link></div>;
  }

  async function placeOrder() {
    setBusy(true);
    setError(null);
    try {
      const payload = {
        shippingAddress: address,
        items: lines.map(({ product, quantity }) => ({
          productId: product.id,
          quantity,
          unitPrice: product.price,
        })),
      };
      const { data } = await api.post('/api/orders', payload); // 202 Accepted
      clear();
      navigate(`/orders/${data.id}`);
    } catch (err) {
      setError(err.response?.data?.detail || err.message || 'Checkout failed');
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="cart-layout">
      <div className="card pad">
        <h2>Checkout</h2>
        <label className="field">Shipping address
          <textarea rows={3} value={address} onChange={(e) => setAddress(e.target.value)} />
        </label>
        <div className="pay-note">
          <strong>Payment:</strong> secure checkout by <strong>PayU</strong>. After you place the
          order you'll be redirected to PayU to pay by card/UPI/netbanking, then brought back here to
          watch your order complete.
        </div>
        {error && <div className="alert error">{error}</div>}
      </div>

      <aside className="card pad summary">
        <h3>Summary</h3>
        {lines.map(({ product, quantity }) => (
          <div className="summary-row" key={product.id}>
            <span>{product.name} × {quantity}</span>
            <span>{formatINR(product.price * quantity)}</span>
          </div>
        ))}
        <div className="summary-row total"><span>Total</span><span>{formatINR(total)}</span></div>
        <button className="btn btn-primary full" onClick={placeOrder} disabled={busy}>
          {busy ? 'Placing…' : `Place order · ${formatINR(total)}`}
        </button>
      </aside>
    </div>
  );
}

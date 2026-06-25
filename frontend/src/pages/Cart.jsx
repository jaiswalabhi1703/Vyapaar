import { Link } from 'react-router-dom';
import { useCart } from '../context/CartContext';
import { formatINR } from '../format';

export default function Cart() {
  const { lines, total, setQuantity } = useCart();

  if (lines.length === 0) {
    return (
      <div className="card pad empty">
        <div className="empty-emoji">🛒</div>
        <h2>Your cart is empty</h2>
        <p className="muted">Add a few things and they’ll show up here.</p>
        <Link to="/" className="btn btn-primary">Browse products</Link>
      </div>
    );
  }

  return (
    <div className="cart-layout">
      <div className="card pad">
        <h2>Your cart</h2>
        {lines.map(({ product, quantity }) => (
          <div className="cart-row" key={product.id}>
            <div className="cart-thumb" style={{ backgroundImage: `url(${product.imageUrl})` }} />
            <div className="cart-info">
              <div className="cart-name">{product.name}</div>
              <div className="muted">{formatINR(product.price)} each</div>
            </div>
            <div className="stepper-control sm">
              <button className="step-btn" onClick={() => setQuantity(product.id, quantity - 1)}>−</button>
              <span className="step-qty">{quantity}</span>
              <button className="step-btn" onClick={() => setQuantity(product.id, quantity + 1)}>+</button>
            </div>
            <div className="cart-line-total">{formatINR(product.price * quantity)}</div>
          </div>
        ))}
      </div>

      <aside className="card pad summary">
        <h3>Order summary</h3>
        <div className="summary-row"><span>Subtotal</span><span>{formatINR(total)}</span></div>
        <div className="summary-row"><span>Shipping</span><span className="free">FREE</span></div>
        <div className="summary-row total"><span>Total</span><span>{formatINR(total)}</span></div>
        <Link to="/checkout" className="btn btn-primary full">Proceed to checkout</Link>
        <p className="muted tiny">Taxes calculated at checkout.</p>
      </aside>
    </div>
  );
}

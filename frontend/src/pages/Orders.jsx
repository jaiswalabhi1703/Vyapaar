import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import api from '../api';
import { formatINR } from '../format';

export default function Orders() {
  const [orders, setOrders] = useState([]);
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    api.get('/api/orders')
      .then((r) => setOrders(r.data))
      .catch(() => setError('Could not load orders.'))
      .finally(() => setLoading(false));
  }, []);

  if (loading) return <p className="muted">Loading…</p>;
  if (error) return <div className="alert error">{error}</div>;
  if (orders.length === 0) {
    return <div className="card pad empty"><div className="empty-emoji">📦</div><h2>No orders yet</h2><Link to="/" className="btn btn-primary">Start shopping</Link></div>;
  }

  return (
    <div className="card pad">
      <h2>My Orders</h2>
      <div className="orders-list">
        {orders.map((o) => (
          <Link to={`/orders/${o.id}`} className="order-card" key={o.id}>
            <div>
              <div className="order-id">Order #{o.id}</div>
              <div className="muted tiny">{new Date(o.createdAt).toLocaleString('en-IN')}</div>
            </div>
            <div className="order-total">{formatINR(o.total)}</div>
            <span className={`pill status-${o.status}`}>{o.status}</span>
          </Link>
        ))}
      </div>
    </div>
  );
}

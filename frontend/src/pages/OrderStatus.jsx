import { useEffect, useRef, useState } from 'react';
import { useParams, useSearchParams, Link } from 'react-router-dom';
import api from '../api';
import SagaStepper from '../components/SagaStepper';
import { formatINR } from '../format';

const TERMINAL = ['COMPLETED', 'CANCELLED'];

// Build a hidden form and POST it to PayU — a full-page navigation to the hosted checkout.
function redirectToPayU(action, params) {
  const form = document.createElement('form');
  form.method = 'POST';
  form.action = action;
  Object.entries(params).forEach(([name, value]) => {
    const input = document.createElement('input');
    input.type = 'hidden';
    input.name = name;
    input.value = value;
    form.appendChild(input);
  });
  document.body.appendChild(form);
  form.submit();
}

export default function OrderStatus() {
  const { id } = useParams();
  const [searchParams] = useSearchParams();
  const paymentResult = searchParams.get('payment'); // success | failed | error (set by PayU callback)
  const [order, setOrder] = useState(null);
  const [error, setError] = useState(null);
  const [redirecting, setRedirecting] = useState(false);
  const timer = useRef(null);
  const redirectedRef = useRef(false);

  useEffect(() => {
    let stopped = false;

    async function poll() {
      try {
        const { data } = await api.get(`/api/orders/${id}`);
        if (stopped) return;
        setOrder(data);

        // While the order is live and we haven't just returned from PayU, check whether a PayU
        // checkout is waiting; if so, hand the customer off to PayU.
        if (!TERMINAL.includes(data.status) && paymentResult !== 'success' && !redirectedRef.current) {
          try {
            const res = await api.get(`/api/payments/payu/checkout/${id}`);
            if (res.status === 200 && res.data && res.data.action && !redirectedRef.current) {
              redirectedRef.current = true;
              setRedirecting(true);
              redirectToPayU(res.data.action, res.data.params);
              return; // navigating away
            }
          } catch {
            /* checkout not ready yet — keep polling the order */
          }
        }

        if (!TERMINAL.includes(data.status)) {
          timer.current = setTimeout(poll, 800);
        }
      } catch {
        if (!stopped) setError('Could not load order.');
      }
    }

    poll();
    return () => { stopped = true; if (timer.current) clearTimeout(timer.current); };
  }, [id, paymentResult]);

  if (error) return <div className="alert error">{error}</div>;
  if (!order) return <p className="muted">Loading order #{id}…</p>;

  return (
    <div className="card pad">
      <div className="order-head">
        <div>
          <h2>Order #{order.id}</h2>
          <div className="muted tiny">Ship to: {order.shippingAddress}</div>
        </div>
        <span className={`pill status-${order.status}`}>{order.status}</span>
      </div>

      {redirecting && <div className="alert">Redirecting you to PayU to complete payment…</div>}
      {paymentResult === 'success' && (
        <div className="alert">Payment received — finalizing your order.</div>
      )}
      {paymentResult === 'failed' && (
        <div className="alert error">Payment was not completed. The order has been cancelled.</div>
      )}
      {paymentResult === 'error' && (
        <div className="alert error">We couldn't verify the payment response. Please contact support.</div>
      )}

      <SagaStepper order={order} />

      <h3 className="mt">Items</h3>
      <div className="order-items">
        {order.items.map((l, i) => (
          <div className="order-item-row" key={i}>
            <span>Product #{l.productId} × {l.quantity}</span>
            <span>{formatINR(l.unitPrice * l.quantity)}</span>
          </div>
        ))}
        <div className="order-item-row total"><span>Total</span><span>{formatINR(order.total)}</span></div>
      </div>

      <p className="mt"><Link to="/orders">← All my orders</Link></p>
    </div>
  );
}

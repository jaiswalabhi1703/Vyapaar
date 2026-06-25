import { useEffect, useState } from 'react';
import api from '../api';
import { useAuth } from '../context/AuthContext';
import { formatINR } from '../format';
import QuantityStepper from '../components/QuantityStepper';

export default function Products() {
  const { isAdmin } = useAuth();
  const [products, setProducts] = useState([]);
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    api.get('/api/products')
      .then((r) => setProducts(r.data))
      .catch(() => setError('Could not load products. Is the gateway up?'))
      .finally(() => setLoading(false));
  }, []);

  return (
    <div>
      <section className="hero">
        <div className="hero-inner">
          <span className="hero-pill">⚡ Saga-powered checkout</span>
          <h1>Gear that just works.</h1>
          <p>Premium tech, fair prices, and an order you can watch move in real time.</p>
        </div>
      </section>

      <div className="section-head">
        <h2>Shop the catalog</h2>
        {isAdmin && <span className="admin-chip">Admin view · stock visible</span>}
      </div>

      {loading && <div className="grid">{Array.from({ length: 6 }).map((_, i) => <div key={i} className="card product skeleton" />)}</div>}
      {error && <div className="alert error">{error}</div>}

      <div className="grid">
        {products.map((p) => (
          <article className="card product" key={p.id}>
            <div className="product-img" style={{ backgroundImage: `url(${p.imageUrl})` }}>
              {!p.inStock && <span className="img-badge out">Sold out</span>}
              {isAdmin && (
                <span className="img-badge stock">{p.availableQty} in stock · {p.reservedQty} held</span>
              )}
            </div>
            <div className="product-body">
              <h3 className="product-name">{p.name}</h3>
              <p className="product-desc">{p.description}</p>
              <div className="product-foot">
                <span className="price">{formatINR(p.price)}</span>
                {!isAdmin && (
                  <span className={`pill ${p.inStock ? 'in' : 'out'}`}>
                    {p.inStock ? 'In stock' : 'Out of stock'}
                  </span>
                )}
              </div>
              <QuantityStepper product={p} disabled={!p.inStock} />
            </div>
          </article>
        ))}
      </div>
    </div>
  );
}

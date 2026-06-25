// Shows "Add to cart" until the item is in the cart, then morphs into a − qty + stepper.
// First add => 1, then + => 2, 3 …; − at 1 removes it and the button returns.
import { useCart } from '../context/CartContext';

export default function QuantityStepper({ product, disabled }) {
  const { quantityOf, add, setQuantity } = useCart();
  const qty = quantityOf(product.id);

  if (qty === 0) {
    return (
      <button className="btn btn-primary full" disabled={disabled} onClick={() => add(product)}>
        {disabled ? 'Out of stock' : 'Add to cart'}
      </button>
    );
  }

  return (
    <div className="stepper-control" role="group" aria-label="quantity">
      <button className="step-btn" onClick={() => setQuantity(product.id, qty - 1)} aria-label="decrease">−</button>
      <span className="step-qty">{qty}</span>
      <button className="step-btn" onClick={() => setQuantity(product.id, qty + 1)} aria-label="increase">+</button>
    </div>
  );
}

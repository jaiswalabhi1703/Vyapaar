import { createContext, useContext, useState } from 'react';

const CartContext = createContext(null);

export function CartProvider({ children }) {
  // { [productId]: { product, quantity } }
  const [items, setItems] = useState({});

  function add(product) {
    setItems((prev) => {
      const existing = prev[product.id];
      const quantity = (existing?.quantity || 0) + 1;
      return { ...prev, [product.id]: { product, quantity } };
    });
  }

  function setQuantity(productId, quantity) {
    setItems((prev) => {
      if (quantity <= 0) {
        const next = { ...prev };
        delete next[productId];
        return next;
      }
      return { ...prev, [productId]: { ...prev[productId], quantity } };
    });
  }

  function clear() {
    setItems({});
  }

  function quantityOf(productId) {
    return items[productId]?.quantity || 0;
  }

  const lines = Object.values(items);
  const total = lines.reduce((sum, l) => sum + l.product.price * l.quantity, 0);
  const count = lines.reduce((sum, l) => sum + l.quantity, 0);

  return (
    <CartContext.Provider value={{ items, lines, total, count, add, setQuantity, quantityOf, clear }}>
      {children}
    </CartContext.Provider>
  );
}

export const useCart = () => useContext(CartContext);

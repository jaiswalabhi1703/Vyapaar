// Indian Rupee formatting, e.g. 24999 -> "₹24,999".
const inr = new Intl.NumberFormat('en-IN', {
  style: 'currency',
  currency: 'INR',
  maximumFractionDigits: 0,
});

export function formatINR(amount) {
  return inr.format(Number(amount || 0));
}

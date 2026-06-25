// Visualizes the distributed saga as a live stepper, including the compensation/rollback path.
// Driven entirely by the order's persisted saga state (reachedStatus + failedStep).

const STEPS = [
  { key: 'INVENTORY', label: 'Inventory', hint: 'Reserve stock', threshold: 1 },
  { key: 'PAYMENT', label: 'Payment', hint: 'Authorize charge', threshold: 2 },
  { key: 'SHIPPING', label: 'Shipping', hint: 'Schedule dispatch', threshold: 4 },
];

// How far the happy path got, as an index.
const REACHED = {
  STARTED: 0,
  INVENTORY_RESERVED: 1,
  PAYMENT_AUTHORIZED: 2,
  SHIPPING_SCHEDULED: 3,
  COMPLETED: 4,
};

const GLYPH = {
  done: '✓',
  active: '…',
  pending: '•',
  failed: '✕',
  undone: '↩',
  skipped: '–',
};

export default function SagaStepper({ order }) {
  const saga = order.saga || { status: 'STARTED', reachedStatus: 'STARTED' };
  const reachedIdx = REACHED[saga.reachedStatus] ?? 0;
  const cancelled =
    order.status === 'CANCELLED' ||
    saga.status === 'COMPENSATING' ||
    saga.status === 'COMPENSATED';

  const pendingThresholds = STEPS.filter((s) => reachedIdx < s.threshold).map((s) => s.threshold);
  const nextThreshold = pendingThresholds.length ? Math.min(...pendingThresholds) : null;

  function stateOf(step) {
    const done = reachedIdx >= step.threshold;
    const failed = saga.failedStep && saga.failedStep.includes(step.key);
    if (failed) return 'failed';
    if (cancelled) return done ? 'undone' : 'skipped';
    if (done) return 'done';
    return step.threshold === nextThreshold ? 'active' : 'pending';
  }

  return (
    <div className="stepper-wrap">
      <div className="stepper">
        {STEPS.map((step, i) => {
          const state = stateOf(step);
          return (
            <div key={step.key} className="step-cell">
              {i > 0 && <div className={`connector ${state}`} />}
              <div className={`step ${state}`}>
                <div className="step-badge">{GLYPH[state]}</div>
                <div className="step-text">
                  <div className="step-label">{step.label}</div>
                  <div className="step-hint">
                    {state === 'undone' ? 'rolled back' : step.hint}
                  </div>
                </div>
              </div>
            </div>
          );
        })}
      </div>

      <div className="saga-outcome">
        {order.status === 'COMPLETED' && <span className="badge ok">Order completed</span>}
        {cancelled && <span className="badge bad">Order cancelled — saga compensated</span>}
        {!cancelled && order.status !== 'COMPLETED' && (
          <span className="badge wip">In progress… ({saga.currentStep})</span>
        )}
        {saga.failureReason && (
          <span className="failure-reason">Reason: {saga.failureReason}</span>
        )}
      </div>
    </div>
  );
}

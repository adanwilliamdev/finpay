import { createIdempotencyKeyTracker, newIdempotencyKey } from './idempotency';

describe('idempotency keys', () => {
  it('generates distinct keys', () => {
    expect(newIdempotencyKey()).not.toEqual(newIdempotencyKey());
  });

  it('reuses the key while the payload is unchanged (retry after an error)', () => {
    const tracker = createIdempotencyKeyTracker();
    const payload = { destinationWalletId: 'WP123', amount: 10 };
    expect(tracker.keyFor(payload)).toEqual(tracker.keyFor({ ...payload }));
  });

  it('uses a new key when the payload changes', () => {
    const tracker = createIdempotencyKeyTracker();
    const first = tracker.keyFor({ destinationWalletId: 'WP123', amount: 10 });
    const second = tracker.keyFor({ destinationWalletId: 'WP123', amount: 11 });
    expect(second).not.toEqual(first);
  });

  it('uses a new key after reset (next operation)', () => {
    const tracker = createIdempotencyKeyTracker();
    const payload = { destinationWalletId: 'WP123', amount: 10 };
    const first = tracker.keyFor(payload);
    tracker.reset();
    expect(tracker.keyFor(payload)).not.toEqual(first);
  });
});

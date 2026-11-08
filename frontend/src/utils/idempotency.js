/**
 * Idempotency-Key support for money-moving requests.
 *
 * The same logical operation must always be sent with the same key, so a retry (network
 * timeout, double click, user pressing "Transfer" again after an error) cannot move money
 * twice. A different payload must get a new key: the backend answers 422 when a key is
 * reused with another payload.
 */
export const newIdempotencyKey = () => {
  const cryptoApi = typeof globalThis !== 'undefined' ? globalThis.crypto : undefined;
  if (cryptoApi && typeof cryptoApi.randomUUID === 'function') {
    return cryptoApi.randomUUID();
  }
  // Fallback for browsers/environments without crypto.randomUUID (e.g. plain HTTP, jsdom).
  return `${Date.now().toString(36)}-${Math.random().toString(36).slice(2)}-${Math.random()
    .toString(36)
    .slice(2)}`;
};

export const createIdempotencyKeyTracker = () => {
  let current = null;

  return {
    /** Same payload -> same key; changed payload -> new key. */
    keyFor(payload) {
      const signature = JSON.stringify(payload);
      if (!current || current.signature !== signature) {
        current = { signature, key: newIdempotencyKey() };
      }
      return current.key;
    },
    /** Call after a successful operation so the next one starts with a fresh key. */
    reset() {
      current = null;
    },
  };
};

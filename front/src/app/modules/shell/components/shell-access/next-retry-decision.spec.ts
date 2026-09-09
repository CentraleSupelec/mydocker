import { MAX_STATUS_ATTEMPTS, nextRetryDecision } from "./next-retry-decision";

describe('nextRetryDecision', () => {
  it('stops at the first refusal, whatever the attempt count', () => {
    expect(nextRetryDecision({ status: 401 }, 1)).toBe('stop');
    expect(nextRetryDecision({ status: 403 }, 1)).toBe('stop');
    expect(nextRetryDecision({ status: 404 }, 1)).toBe('stop');
  });

  it('retries a server error until the last allowed attempt', () => {
    for (let attempts = 1; attempts < MAX_STATUS_ATTEMPTS; attempts++) {
      expect(nextRetryDecision({ status: 500 }, attempts)).toBe('retry');
    }
  });

  it('gives up on the fifth server error', () => {
    expect(nextRetryDecision({ status: 503 }, MAX_STATUS_ATTEMPTS)).toBe('give_up');
    expect(nextRetryDecision({ status: 503 }, MAX_STATUS_ATTEMPTS + 1)).toBe('give_up');
  });

  it('treats a network error, which carries no status, as retryable', () => {
    expect(nextRetryDecision({ status: 0 }, 1)).toBe('retry');
    expect(nextRetryDecision(null, 1)).toBe('retry');
    expect(nextRetryDecision({ status: 0 }, MAX_STATUS_ATTEMPTS)).toBe('give_up');
  });
});

export type RetryDecision = 'retry' | 'give_up' | 'stop';

/** How many failures in a row the panel reads through before it declares the status unreadable. */
export const MAX_STATUS_ATTEMPTS = 5;

/**
 * Decides what a failing status read leads to. Only a server error, or a network error, which
 * carries no status, is worth reading again, and then a bounded number of times. Anything else is an
 * answer: the back end will keep giving it, so retrying it, and above all re-issuing a creation
 * request on it, only multiplies the load.
 *
 * @param attempts failures in a row so far, including the one being decided on.
 */
export function nextRetryDecision(
  error: { status?: number } | null | undefined,
  attempts: number
): RetryDecision {
  const status = error?.status ?? 0;

  if (0 !== status && status < 500) {
    return 'stop';
  }

  return attempts >= MAX_STATUS_ATTEMPTS ? 'give_up' : 'retry';
}

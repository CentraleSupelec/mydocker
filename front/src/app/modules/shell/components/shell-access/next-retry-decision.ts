export type RetryDecision = 'retry' | 'give_up' | 'stop';

/** How many failures in a row the panel reads through before it declares the status unreadable. */
export const MAX_STATUS_ATTEMPTS = 5;

/**
 * Decides what a failing status read leads to. A refusal is an answer: the back end will keep
 * refusing, so retrying it, and above all re-issuing a creation request, only multiplies the load.
 * A server or network error may pass, so it is read again a bounded number of times.
 *
 * @param attempts failures in a row so far, including the one being decided on.
 */
export function nextRetryDecision(
  error: { status?: number } | null | undefined,
  attempts: number
): RetryDecision {
  const status = error?.status ?? 0;

  if (status >= 400 && status < 500) {
    return 'stop';
  }

  return attempts >= MAX_STATUS_ATTEMPTS ? 'give_up' : 'retry';
}

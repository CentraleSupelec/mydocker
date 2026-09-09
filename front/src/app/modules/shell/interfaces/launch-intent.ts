/**
 * What the student asked the dashboard to do with an environment. Observing that an environment is
 * already running is not an intent: that is what `active` says.
 */
export type LaunchIntent = 'none' | 'start';

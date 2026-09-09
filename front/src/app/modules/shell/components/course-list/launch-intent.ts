import { IBasicCourseWithSession } from "../../interfaces/course";
import { LaunchIntent } from "../../interfaces/launch-intent";

/**
 * The running state of the course is deliberately not an input here. A start asked for an
 * environment that already runs is still a start, and the panel decides that it needs no creation
 * request; conflating the two is the defect this function replaces.
 */
export function launchIntentFor(
  element: IBasicCourseWithSession,
  launchSessionId: number | undefined
): LaunchIntent {
  if (undefined === launchSessionId || !element.sessions || 0 === element.sessions.length) {
    return 'none';
  }

  return element.sessions[0].id === launchSessionId ? 'start' : 'none';
}

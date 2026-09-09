import { IBasicCourseWithSession } from "../../interfaces/course";
import { ISession } from "../../interfaces/session";
import { launchIntentFor } from "./launch-intent";

function course(id: number, sessionIds: number[]): IBasicCourseWithSession {
  return {
    id,
    sessions: sessionIds.map((sessionId) => ({ id: sessionId } as ISession)),
  } as IBasicCourseWithSession;
}

describe('launchIntentFor', () => {
  it('asks to start the course whose first session carries the launch parameter', () => {
    expect(launchIntentFor(course(1, [11]), 11)).toBe('start');
  });

  it('asks nothing when no launch parameter was consumed', () => {
    expect(launchIntentFor(course(1, [11]), undefined)).toBe('none');
  });

  it('asks nothing for another course', () => {
    expect(launchIntentFor(course(2, [22]), 11)).toBe('none');
  });

  it('reads the first session only, as the dashboard displays only that one', () => {
    expect(launchIntentFor(course(1, [11, 12]), 12)).toBe('none');
  });

  it('asks nothing for a course without a session', () => {
    expect(launchIntentFor(course(1, []), 11)).toBe('none');
  });
});

import { IBasicCourseWithSession } from "../../interfaces/course";
import { ISession } from "../../interfaces/session";
import { excludePlanifiedFromPast } from "./exclude-planified-from-past";

function course(id: number, sessionIds: number[]): IBasicCourseWithSession {
  return {
    id,
    sessions: sessionIds.map((sessionId) => ({ id: sessionId } as ISession)),
  } as IBasicCourseWithSession;
}

describe('excludePlanifiedFromPast', () => {
  it('drops a course that also appears in the planified list', () => {
    const planified = [course(1, [11])];
    const past = [course(1, [10]), course(2, [20])];

    const result = excludePlanifiedFromPast(planified, past);

    expect(result.map((element) => element.id)).toEqual([2]);
  });

  it('keeps a course that has past sessions only', () => {
    const past = [course(2, [20])];

    const result = excludePlanifiedFromPast([], past);

    expect(result).toEqual(past);
  });

  it('never adds a planified course to the past list', () => {
    const result = excludePlanifiedFromPast([course(3, [30])], []);

    expect(result).toEqual([]);
  });

  it('is safe on two empty lists', () => {
    expect(excludePlanifiedFromPast([], [])).toEqual([]);
  });

  it('is safe when a list is missing', () => {
    expect(excludePlanifiedFromPast(undefined, [course(4, [40])]).map((element) => element.id))
      .toEqual([4]);
    expect(excludePlanifiedFromPast([course(4, [40])], undefined)).toEqual([]);
  });
});

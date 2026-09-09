import { IBasicCourseWithSession } from "../../interfaces/course";

/**
 * A course with both a past and a future session used to render in both dashboard blocks, and each
 * block polled it separately. The planified block wins: it shows the next start date.
 */
export function excludePlanifiedFromPast(
  planified: IBasicCourseWithSession[] | undefined,
  past: IBasicCourseWithSession[] | undefined
): IBasicCourseWithSession[] {
  if (!past) {
    return [];
  }
  if (!planified) {
    return past;
  }

  const planifiedIds = new Set(planified.map((course) => course.id));

  return past.filter((course) => !planifiedIds.has(course.id));
}

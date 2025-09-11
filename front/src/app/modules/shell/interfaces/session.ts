import { IBasicCourse } from "./course";

export interface ISession {
  id: number;
  startDateTime: number;
  endDateTime: number;
  blockContainerCreationBeforeStartTime: boolean;
  destroyContainerAfterEndTime: boolean;
  course: IBasicCourse;
}

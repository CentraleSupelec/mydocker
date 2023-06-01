import { IPort } from "../../ports-form/interfaces/port";
import { ICourseDisplay } from "../../admin-course/interfaces/course-display";
import { ISession } from "./session";

export interface IBasicCourse {
  id: number;
  title: string;
  description: string;
  creator: string;
  ports: IPort[];
  studentWorkIsSaved: boolean;
  allowStudentToSubmit: boolean;
  displayOptions: ICourseDisplay;
  shutdownAfterMinutes?: number;
  warnShutdownMinutes?: number;
}

export interface IBasicCourseWithSession extends IBasicCourse {
  sessions: ISession[];
}

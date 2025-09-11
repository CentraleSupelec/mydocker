import { IPort } from "../../ports-form/interfaces/port";
import { ICourseDisplay } from "../../admin-course/interfaces/course-display";
import { ISession } from "./session";

export interface IBasicCourse {
  id: number;
  uuid: string;
  title: string;
  description: string;
  creator: string;
  ports: IPort[];
  studentWorkIsSaved: boolean;
  allowStudentToSubmit: boolean;
  displayOptions: ICourseDisplay;
  shutdownAfterMinutes?: number;
  warnShutdownMinutes?: number;
  lastStartDate: string;
  createdAt: string
  externalAccess: boolean;
  externalAccessExpirationDate: string;
}

export interface IBasicCourseWithSession extends IBasicCourse {
  sessions: ISession[];
}

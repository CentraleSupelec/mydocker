import { IPort } from "../../ports-form/interfaces/port";
import {IAdminSession, IAdminUpdateSession} from "../../sessions-form/interfaces/admin-session";
import { ICourseDisplay } from "./course-display";

export enum CourseStatus {
  DRAFT = 'DRAFT',
  TEST = 'TEST',
  READY = 'READY',
  ARCHIVED = 'ARCHIVED'
}

export interface IAdminUpdateCourse extends IAdminCourseBase {
  sessions: IAdminUpdateSession[];
}
export interface IAdminCourseBase {
  title: string;
  description: string;
  ports: IPort[];
  status: CourseStatus;

  dockerImage: string;
  nanoCpusLimit: number;
  memoryBytesLimit: number;
  computeTypeId: number;

  saveStudentWork: boolean;
  workdirSize: number;
  workdirPath: string;
  allowStudentToSubmit: boolean;

  displayOptions: ICourseDisplay;

  shutdownAfterMinutes: number;
  warnShutdownMinutes: number;
}

export interface IAdminCourse extends IAdminCourseBase {
  id: number;
  creator: string;

  updatedOn: number;
  createdOn: number;

  link: string;
  sessions: IAdminSession[];
}

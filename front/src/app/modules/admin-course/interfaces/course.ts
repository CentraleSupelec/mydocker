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
  command: string;

  saveStudentWork: boolean;
  workdirSize: number | null;
  workdirPath: string | null;
  allowStudentToSubmit: boolean;
  useStudentVolume: boolean;
  studentVolumePath: string | null;
  uid: string | null;

  visible: boolean;

  displayOptions: ICourseDisplay;

  shutdownAfterMinutes: number;
  warnShutdownMinutes: number;

  externalAccess: boolean;
  externalAccessExpirationDate: string;
  numberOfUsers: number;
  numberOfConnectedUsers: number;
  numberOfRecentUsers: number;
}

export interface IAdminCourse extends IAdminCourseBase {
  id: number;
  uuid: string;
  creator: string;

  updatedOn: number;
  createdOn: number;

  link: string;
  sessions: IAdminSession[];
}

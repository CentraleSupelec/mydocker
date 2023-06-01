import { IAdminSession } from "../../sessions-form/interfaces/admin-session";
import { IAdminCourse } from "../../admin-course/interfaces/course";

export interface IResourceDescription {
  ovhResourceId: number;
  count: number;
}

export interface ISessionWithResources extends IAdminSession {
  course: IAdminCourse;
  resources: IResourceDescription[];
}

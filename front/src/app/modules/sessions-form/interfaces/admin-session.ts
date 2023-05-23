import { IDeploymentSummary } from "../../deployment/interfaces/deployment";

export interface IAdminSession extends IAdminUpdateSession {
  launchDeployment: IDeploymentSummary;
  cleanDeployment: IDeploymentSummary;
}

export interface IAdminUpdateSession {
  id: number;
  startDateTime: number;
  endDateTime: number;
  blockContainerCreationBeforeStartTime: boolean;
  destroyContainerAfterEndTime: boolean;
  studentNumber: number;
}

export interface ISessionsById {
  [sessionId: number]: IAdminSession
}

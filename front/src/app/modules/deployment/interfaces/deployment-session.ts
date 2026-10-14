import { ISession } from "../../shell/interfaces/session";

// A session as the deployment endpoints list it, with the cluster of its course (null when unassigned).
export interface IDeploymentSession extends ISession {
  cluster: number | null;
}

import { IRegionWorker, IRegionWorkerWithSession } from "./region-worker";
import { ISession } from "../../shell/interfaces/session";
import { ICluster } from "../../cluster/interfaces/cluster";

export interface IUpdateDeployment {
  workers: IRegionWorker[];
  startDateTime: number;
  sessions: number[];
  type: 'launch' | 'clean';
}

export interface IDeployment {
  id: number;
  updatedOn: number;
  workers: IRegionWorkerWithSession[];
  startDateTime: number;
  sessions: ISession[];
  type: 'launch' | 'clean';
  description: string;
  cluster: number
}

export interface IDeploymentSummary {
  id: number;
  updatedOn: number;
  startDateTime: number;
  type: 'launch' | 'clean';
  description: string;
}

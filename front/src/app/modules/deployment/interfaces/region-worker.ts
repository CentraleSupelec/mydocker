import { ISession } from "../../shell/interfaces/session";

export interface IRegionWorker {
  id: number;
  region: string;
  resource: number;
  count: number;
  computeTypeId: number;
}

export interface IRegionWorkerWithSession extends IRegionWorker {
  sessions: ISession[];
}

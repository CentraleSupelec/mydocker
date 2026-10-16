export interface IDeploymentStatus {
  id: number;
  cluster: number | null;
  logs: string;
  buildErrors: string;
  status: 'DONE' | 'ERROR' | 'RUNNING' | 'CREATED' | 'SKIPPED';
  createdOn: number;
  updatedOn: number;
}

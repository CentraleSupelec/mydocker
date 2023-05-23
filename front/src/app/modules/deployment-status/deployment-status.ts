export interface IDeploymentStatus {
  id: number;
  logs: string;
  buildErrors: string;
  status: 'DONE' | 'ERROR' | 'RUNNING' | 'CREATED' | 'SKIPPED';
  createdOn: number;
  updatedOn: number;
}

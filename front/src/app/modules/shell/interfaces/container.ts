import { IContainerPort } from "./container-port";

export enum ContainerStatus {
  OK = 'OK',
  KO = 'KO',
  PENDING = 'PENDING',
  CHECKING = 'CHECKING'
}

export enum ContainerSwarmState {
  UNKNOWN = 'UNKNOWN',
  NEW = 'NEW',
  PENDING = 'PENDING',
  ASSIGNED = 'ASSIGNED',
  ACCEPTED = 'ACCEPTED',
  PREPARING = 'PREPARING',
  READY = 'READY',
  STARTING = 'STARTING',
  RUNNING = 'RUNNING',
  COMPLETE = 'COMPLETE',
  SHUTDOWN = 'SHUTDOWN',
  FAILED = 'FAILED',
  REJECTED = 'REJECTED',
  ORPHANED = 'ORPHANED',
}

export interface IContainer {
  username: string;
  password: string;
  ip: string;
  ports: IContainerPort[];
  status: keyof typeof ContainerStatus;
  state?: keyof typeof ContainerSwarmState;
  deletionTime?: number;
  creationError?: string;
  errorParams: { [key: string]: string};
}

export interface ITaskLog {
  taskId: string;
  slot: number;
  node: string;
  createdAt: string | null;
  logs: string;
  truncated: boolean;
  readError: string;
}

export interface IServiceLogs {
  name: string;
  image: string;
  tasks: ITaskLog[];
  omittedTasks: number;
}

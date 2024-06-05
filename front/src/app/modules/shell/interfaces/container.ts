import { IContainerPort } from "./container-port";

export interface IContainer {
  username: string;
  password: string;
  ip: string;
  ports: IContainerPort[];
  status: 'OK' | 'KO';
  deletionTime?: number;
  creationError?: string;
  errorParams: { [key: string]: string};
}

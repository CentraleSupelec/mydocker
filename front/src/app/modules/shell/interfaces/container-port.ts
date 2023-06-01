import { IPort } from "../../ports-form/interfaces/port";

export interface IContainerPort extends IPort {
  portMapTo: number;
}

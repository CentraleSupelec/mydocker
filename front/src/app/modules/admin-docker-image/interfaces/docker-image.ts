import { IPort } from "../../ports-form/interfaces/port";

export interface IDockerImage {
  id: number;
  name: string;
  description: string;
  dockerFile: string;
  wrapperScript: string;
  contextFolderName: string;
  ports: IPort[];
  lastStatus: string;
  creator: string;
  visible: boolean;
  createdOn: number;
}

export interface IUpdateDockerImage {
  description: string;
  dockerFile: string;
  wrapperScript: string;
  ports: IPort[];
  contextFolder: File | null;
  visible: boolean;
}

export interface ICreationDockerImage extends IUpdateDockerImage {
  name: string;
}

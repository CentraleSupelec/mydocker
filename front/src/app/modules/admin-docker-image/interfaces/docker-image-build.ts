import { BuildStatus } from "./build-status";

export interface IDockerImageBuild {
  id: number;
  status: BuildStatus;
  buildErrors: string;
  logs: string;
  updatedOn: number;
  imageName: string;
}

import { IOvhResource } from '../../sessions-resources/interfaces/ovh-resource';

export interface IOvhRegion {
  region: string;
  imageId: string;
}
export enum IStorageBackend {
  LOCAL = 'Local',
  NFS = 'NFS',
  RBD = 'RBD',
}
export interface IComputeType {
  displayName: string;
  technicalName: string;
  gpu: boolean;
  minIdleNodesCount: number;
  maxNodesCount: number;
  id: number;
  autoscalingResource: IOvhResource;
  autoscalingRegions: Array<IOvhRegion>;
  manualNodesCount: number;
  storageBackend: IStorageBackend;
}

export interface IComputeTypeUpdateDto {
  displayName: string;
  technicalName: string;
  gpu: boolean;
  autoscalingRegion: string;
  autoscalingResource: number;
  minIdleNodesCount: number;
  maxNodesCount: number;
  manualNodesCount: number;
}

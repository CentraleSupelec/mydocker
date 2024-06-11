import { IOvhResource } from '../../sessions-resources/interfaces/ovh-resource';

export interface IOvhRegion {
  region: string;
  imageId: string;
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

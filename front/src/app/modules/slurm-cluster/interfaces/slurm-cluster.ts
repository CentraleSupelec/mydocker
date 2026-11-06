export interface ISlurmCluster {
  id: number;
  displayName: string;
  technicalName: string;
  apiEndpoint: string;
  secretKey: string;
}

export interface ISlurmClusterUpdateDto {
  displayName: string;
  technicalName: string;
  apiEndpoint: string;
  secretKey: string;
}

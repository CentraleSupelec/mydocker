export interface IClusterBasicInformaion {
  id: number;
  name: string;
}

export interface ICluster extends IClusterBasicInformaion {
  goAddress: string;
  goPort: number;
  terraformStateUrl: string;
  deploymentEnabled: boolean;
}

export interface IClusterUpdateDto {
  name: string;
  goAddress: string;
  goPort: number;
  terraformStateUrl: string;
  deploymentEnabled: boolean;
}

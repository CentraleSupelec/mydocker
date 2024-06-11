export interface IPort {
  description: string;
  mapPort: number;
  connectionType: ConnectionType;
  requiredToAccessContainer: boolean;
  hostname: string;
}

export enum ConnectionType {
  HTTP = 'HTTP',
  SSH = 'SSH',
  OTHER = 'AUTRE',
}

export interface IPortDisplay {
  type: string;
}

export interface IHttpPortDisplay extends IPortDisplay {
  url: string;
  title: string;
}

export interface ICourseDisplay {
  displayUsername?: boolean;
  displayPassword?: boolean;
  displayPorts?: {[id: string]: boolean};
  customPortsDisplay?: IPortDisplay[];
}

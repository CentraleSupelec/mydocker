export interface IPortDisplay {
  type: string;
}

export enum CustomDisplayType {
  BUTTON = 'BOUTON',
  TEXT = 'TEXTE'
}

export interface IHttpPortDisplay extends IPortDisplay {
  url: string;
  title: string;
  customDisplayType: CustomDisplayType
}

export interface ICourseDisplay {
  displayUsername?: boolean;
  displayPassword?: boolean;
  displayPorts?: {[id: string]: boolean};
  customPortsDisplay?: IPortDisplay[];
}

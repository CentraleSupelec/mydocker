import { IContainerPort } from "./container-port";

export enum ContainerStatus {
  OK = 'OK',
  KO = 'KO',
  PENDING = 'PENDING',
  CHECKING = 'CHECKING'
}

export enum ContainerSwarmState {
  UNKNOWN = 'UNKNOWN',
  NEW = 'NEW',
  PENDING = 'PENDING',
  ASSIGNED = 'ASSIGNED',
  ACCEPTED = 'ACCEPTED',
  PREPARING = 'PREPARING',
  READY = 'READY',
  STARTING = 'STARTING',
  RUNNING = 'RUNNING',
  COMPLETE = 'COMPLETE',
  SHUTDOWN = 'SHUTDOWN',
  FAILED = 'FAILED',
  REJECTED = 'REJECTED',
  ORPHANED = 'ORPHANED',
}

export interface IContainer {
  username: string;
  password: string;
  ip: string;
  ports: IContainerPort[];
  status: keyof typeof ContainerStatus;
  state?: keyof typeof ContainerSwarmState;
  deletionTime?: number;
  creationError?: string;
  errorParams: { [key: string]: string};
}

export const ContainerSwarmStateMessages = {
  [ContainerSwarmState.NEW]: 'Création de l\'environnement',
  [ContainerSwarmState.PENDING]: 'Recherche d\'un nœud de calcul',
  [ContainerSwarmState.ASSIGNED]: 'Assignation à un nœud',
  [ContainerSwarmState.ACCEPTED]: 'Acceptation par le nœud',
  [ContainerSwarmState.PREPARING]: 'Téléchargement de l\'environnement sur le noeud',
  [ContainerSwarmState.READY]: 'Environnement prêt à démarrer',
  [ContainerSwarmState.STARTING]: 'Initialisation en cours',
  [ContainerSwarmState.RUNNING]: 'En attente de l’interface',
  [ContainerSwarmState.COMPLETE]: 'Environnement terminé',
  [ContainerSwarmState.FAILED]: 'Échec de l\'environnement',
  [ContainerSwarmState.SHUTDOWN]: 'Arrêt en cours',
  [ContainerSwarmState.REJECTED]: 'Environnement rejeté',
  [ContainerSwarmState.ORPHANED]: 'Environnement orphelin',
  [ContainerSwarmState.UNKNOWN]: 'État inconnu',
}

import { IUser } from "./user";

export interface IPermission {
  id: number;
  type: 'edit' | 'view';
  user: IUser;
}

export interface IUpdatePermission {
  id: number;
  type: 'edit' | 'view';
  userId: number;
}

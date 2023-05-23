export interface IUser extends IUpdateUser {
  id: number;
}

export interface IUpdateUser {
  email: string;
  name: string;
  lastname: string;
  role: string;
}

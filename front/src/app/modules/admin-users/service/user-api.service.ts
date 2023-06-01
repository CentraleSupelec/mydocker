import { Inject, Injectable } from '@angular/core';
import { Observable } from "rxjs";
import { IPageResponse } from "../../utils/page";
import { HttpClient, HttpParams } from "@angular/common/http";
import { APP_CONFIG, IAppConfig } from "../../../app-config";
import { IUpdateUser, IUser } from "../../permissions/interfaces/user";

@Injectable({
  providedIn: 'root'
})
export class UserApiService {
  constructor(
    @Inject(APP_CONFIG) private readonly config: IAppConfig,
    private readonly httClient: HttpClient,
  ) { }

  getUsers(
    email: string,
    roles: string[],
    page: number | undefined,
    limit: number | undefined,
    sort: string | undefined,
    direction: 'asc' | 'desc' | undefined,
  ): Observable<IPageResponse<IUser>> {
    let params: HttpParams = new HttpParams()
      .append('size', limit ? limit.toString() : '')
      .append('page', page? page.toString(): '')
      .append('sort', `${sort},${direction}`)
      .append('roles', roles.join(', '))
      .append('search', email);
    return this.httClient.get<IPageResponse<IUser>>(
      `${this.config.back_url}user`, {
        params: params,
      })
  }

  createUser(user: IUpdateUser): Observable<void> {
    return this.httClient.post<void>(`${this.config.back_url}user`, user);
  }

  editUser(id: number | undefined, user: IUpdateUser): Observable<void> {
    return this.httClient.put<void>(`${this.config.back_url}user/${id}`, user);
  }

  getUser(id: number): Observable<IUser> {
    return this.httClient.get<IUser>(`${this.config.back_url}user/${id}`);
  }
}

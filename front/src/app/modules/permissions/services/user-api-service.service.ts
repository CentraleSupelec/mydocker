import { Inject, Injectable } from '@angular/core';
import { HttpClient } from "@angular/common/http";
import { APP_CONFIG, IAppConfig } from "../../../app-config";
import { IUser } from "../interfaces/user";
import { Observable } from "rxjs";

@Injectable({
  providedIn: 'root'
})
export class UserApiServiceService {

  constructor(
    private readonly httpClient: HttpClient,
    @Inject(APP_CONFIG) private readonly config: IAppConfig,
  ) { }

  public searchUser(email: string): Observable<IUser[]> {
      return this.httpClient.get<IUser[]>(`${this.config.back_url}user/search`, {
        params: {
          search: email
        }
      });
  }
}

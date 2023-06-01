import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, Resolve, RouterStateSnapshot } from '@angular/router';
import { Observable } from 'rxjs';
import { IUser } from "../../permissions/interfaces/user";
import { UserApiService } from "./user-api.service";

@Injectable({
  providedIn: 'root'
})
export class UserResolver implements Resolve<IUser> {
  constructor(
    private readonly userApiService: UserApiService,
  ) {
  }

  resolve(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<IUser> {
    const id = parseInt(<string>route.paramMap.get('id'), 10);
    return this.userApiService.getUser(id);
  }
}

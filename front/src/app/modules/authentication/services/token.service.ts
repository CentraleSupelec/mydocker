import { Injectable } from '@angular/core';
import { Observable, of, ReplaySubject, Subject } from "rxjs";
import { LocalStorageService } from "../../utils/services/local-storage.service";
import { NgxPermissionsService } from "ngx-permissions";
import { UserMeService } from "./user-me.service";
import { IJWTToken } from "../interfaces/jwt-token";
import { JwtHelperService } from "@auth0/angular-jwt";
import { catchError, map, tap } from "rxjs/operators";
import { HttpErrorResponse } from "@angular/common/http";

@Injectable({
  providedIn: 'root'
})
export class TokenService {
  private readonly localStorageTokenKey = 'token';
  private tokenSubject: Subject<IJWTToken | null> = new ReplaySubject<IJWTToken | null>(1);
  private readonly jwtHelper: JwtHelperService = new JwtHelperService();
  private token: IJWTToken | null = null;

  constructor(
    private readonly storageService: LocalStorageService,
    private readonly permissionsService: NgxPermissionsService,
    private readonly userMeService: UserMeService,
  ) {
  }

  get tokenChanges$(): Subject<IJWTToken | null> {
    return this.tokenSubject;
  }

  signInFromStorage(): Observable<IJWTToken | null> {
    const savedToken = this.storageService.get(this.localStorageTokenKey);
    if (this.token) {  // if already loaded do nothing
      return of(null);
    } else {
      return this.loadToken(savedToken);
    }
  }

  isSignedIn(): boolean {
    return this.token !== null && this.token.expirationDate > new Date();
  }

  getToken(): IJWTToken | null {
    return this.token;
  }

  loadToken(encodedToken: string | null | undefined): Observable<IJWTToken | null> {
    if (encodedToken == null) {
      return of(null);
    }

    let decodedToken;
    try {
      decodedToken = this.jwtHelper.decodeToken(encodedToken);
    } catch (e) {
      console.warn('cannot decode jwtToken');
      this.cleanStorage();
      return of(null);
    }

    const expirationDate = new Date(0);
    expirationDate.setUTCSeconds(decodedToken.exp);

    const token = {
      encodedToken,
      decodedToken,
      expirationDate,
    };
    this.token = token;
    this.storageService.set(this.localStorageTokenKey, encodedToken);
    return this.reloadPermissions()
      .pipe(
        catchError((err) => {
          if (err instanceof HttpErrorResponse) {
            return of(null);
          } else {
            throw err;
          }
        }),
        map(() => this.token),
        tap(() => {
          this.tokenSubject.next(token);
        }),
      );
  }

  private reloadPermissions(): Observable<any> {
    return this.userMeService.getUserMe()
      .pipe(
        tap((roles: string[]) => {
          this.permissionsService.loadPermissions(roles);
        }),
      );
  }

  public cleanStorage(): void {
    this.token = null;
    this.tokenSubject.next(null);
    this.storageService.remove(this.localStorageTokenKey);
    this.permissionsService.flushPermissions();
  }
}

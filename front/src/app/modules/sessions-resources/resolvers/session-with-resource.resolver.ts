import { Injectable } from '@angular/core';
import {
  Router, Resolve,
  RouterStateSnapshot,
  ActivatedRouteSnapshot
} from '@angular/router';
import { Observable, of } from 'rxjs';
import { ISessionWithResources } from "../interfaces/session-with-resources";
import { SessionWithResourcesApiService } from "../services/session-with-resources-api.service";

@Injectable({
  providedIn: 'root'
})
export class SessionWithResourceResolver implements Resolve<ISessionWithResources> {
  constructor(
    private readonly sessionWitResourceApiService: SessionWithResourcesApiService,
  ) {
  }
  resolve(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<ISessionWithResources> {
    const id = parseInt(<string>route.paramMap.get('id'), 10);
    return this.sessionWitResourceApiService.getSession(id);
  }
}

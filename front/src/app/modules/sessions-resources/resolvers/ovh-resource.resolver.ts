import { Injectable } from '@angular/core';
import {
  Router, Resolve,
  RouterStateSnapshot,
  ActivatedRouteSnapshot
} from '@angular/router';
import { Observable, of } from 'rxjs';
import { IOvhResource } from "../interfaces/ovh-resource";
import { OvhResourceApiService } from "../services/ovh-resource-api.service";

@Injectable({
  providedIn: 'root'
})
export class OvhResourceResolver implements Resolve<IOvhResource[]> {
  constructor(
    private readonly ovhResourceApiService: OvhResourceApiService
  ) {
  }
  resolve(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<IOvhResource[]> {
    return this.ovhResourceApiService.getResources();
  }
}

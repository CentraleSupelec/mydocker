import { Injectable } from '@angular/core';
import {
  Router, Resolve,
  RouterStateSnapshot,
  ActivatedRouteSnapshot
} from '@angular/router';
import { Observable, of } from 'rxjs';
import { IDeployment } from "../interfaces/deployment";
import { DeploymentApiService } from "../services/deployment-api.service";

@Injectable({
  providedIn: 'root'
})
export class DeploymentsResolver implements Resolve<IDeployment[]> {

  constructor(
    private readonly deploymentApiService: DeploymentApiService,
  ) {
  }

  resolve(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<IDeployment[]> {
    return this.deploymentApiService.getDeployments();
  }
}

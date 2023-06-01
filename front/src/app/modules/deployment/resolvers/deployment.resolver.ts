import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, Resolve, RouterStateSnapshot } from '@angular/router';
import { Observable } from 'rxjs';
import { IDeployment } from "../interfaces/deployment";
import { DeploymentApiService } from "../services/deployment-api.service";

@Injectable({
  providedIn: 'root'
})
export class DeploymentResolver implements Resolve<IDeployment> {
  constructor(
    private readonly deploymentApiService: DeploymentApiService,
  ) {
  }


  resolve(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<IDeployment> {
    const id = parseInt(<string>route.paramMap.get('id'), 10);
    return this.deploymentApiService.getDeployment(id);
  }
}

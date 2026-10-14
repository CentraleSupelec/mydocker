import { Injectable } from '@angular/core';
import {
  Resolve,
  RouterStateSnapshot,
  ActivatedRouteSnapshot
} from '@angular/router';
import { Observable } from 'rxjs';
import { IClusterBasicInformaion } from '../interfaces/cluster';
import { ClustersApiService } from '../services/clusters-api.service';

@Injectable({
  providedIn: 'root'
})
export class ClustersBasicInformationResolver implements Resolve<Array<IClusterBasicInformaion>> {
  constructor(private readonly clustersApiService: ClustersApiService) {
  }

  resolve(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<Array<IClusterBasicInformaion>> {
    return this.clustersApiService.getClustersBasicInformation();
  }
}

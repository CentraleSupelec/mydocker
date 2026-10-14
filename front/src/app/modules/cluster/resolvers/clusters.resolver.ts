import { Injectable } from '@angular/core';
import {
  Resolve,
  RouterStateSnapshot,
  ActivatedRouteSnapshot
} from '@angular/router';
import { Observable } from 'rxjs';
import { ICluster } from '../interfaces/cluster';
import { ClustersApiService } from '../services/clusters-api.service';

@Injectable({
  providedIn: 'root'
})
export class ClustersResolver implements Resolve<Array<ICluster>> {
  constructor(private readonly clustersApiService: ClustersApiService) {
  }

  resolve(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<Array<ICluster>> {
    return this.clustersApiService.getClusters();
  }
}

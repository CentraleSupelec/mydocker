import { Injectable } from '@angular/core';
import {
  Resolve,
  RouterStateSnapshot,
  ActivatedRouteSnapshot
} from '@angular/router';
import { Observable } from 'rxjs';
import { ClustersApiService } from '../services/clusters-api.service';
import { ICluster } from '../interfaces/cluster';

@Injectable({
  providedIn: 'root'
})
export class ClusterResolver implements Resolve<ICluster> {
  constructor(private readonly clustersApiService: ClustersApiService) {
  }
  resolve(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<ICluster> {
    const id = parseInt(<string>route.paramMap.get('id'), 10);
    return this.clustersApiService.getCluster(id);
  }
}

import { Injectable } from '@angular/core';
import {
  Resolve,
  RouterStateSnapshot,
  ActivatedRouteSnapshot,
} from '@angular/router';
import { Observable } from 'rxjs';
import { SlurmClustersApiService } from '../services/slurm-clusters-api.service';
import { ISlurmCluster } from '../interfaces/slurm-cluster';

@Injectable({
  providedIn: 'root',
})
export class SlurmClusterResolver implements Resolve<ISlurmCluster> {
  constructor(
    private readonly slurmClustersApiService: SlurmClustersApiService
  ) {}
  resolve(
    route: ActivatedRouteSnapshot,
    state: RouterStateSnapshot
  ): Observable<ISlurmCluster> {
    const id = parseInt(<string>route.paramMap.get('id'), 10);
    return this.slurmClustersApiService.getSlurmCluster(id);
  }
}

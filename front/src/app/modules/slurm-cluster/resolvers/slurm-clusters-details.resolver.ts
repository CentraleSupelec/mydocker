import { Injectable } from '@angular/core';
import {
  Resolve,
  RouterStateSnapshot,
  ActivatedRouteSnapshot,
} from '@angular/router';
import { Observable } from 'rxjs';
import { ISlurmCluster } from '../interfaces/slurm-cluster';
import { SlurmClustersApiService } from '../services/slurm-clusters-api.service';

// Resolves the admin management list (id + displayName + technicalName +
// apiEndpoint, no secret). Separate from SlurmClustersResolver, which serves
// the teacher-facing course dropdown with the minimal projection.
@Injectable({
  providedIn: 'root',
})
export class SlurmClustersDetailsResolver
  implements Resolve<Array<ISlurmCluster>>
{
  constructor(
    private readonly slurmClustersApiService: SlurmClustersApiService
  ) {}

  resolve(
    route: ActivatedRouteSnapshot,
    state: RouterStateSnapshot
  ): Observable<Array<ISlurmCluster>> {
    return this.slurmClustersApiService.getSlurmClusterDetails();
  }
}

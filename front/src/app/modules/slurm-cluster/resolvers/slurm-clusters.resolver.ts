import { Injectable } from '@angular/core';
import {
  Resolve,
  RouterStateSnapshot,
  ActivatedRouteSnapshot,
} from '@angular/router';
import { NgxPermissionsService } from 'ngx-permissions';
import { from, Observable, of } from 'rxjs';
import { catchError, switchMap } from 'rxjs/operators';
import { ISlurmCluster } from '../interfaces/slurm-cluster';
import { SlurmClustersApiService } from '../services/slurm-clusters-api.service';

@Injectable({
  providedIn: 'root',
})
export class SlurmClustersResolver implements Resolve<Array<ISlurmCluster>> {
  constructor(
    private readonly slurmClustersApiService: SlurmClustersApiService,
    private readonly permissionsService: NgxPermissionsService
  ) {}

  // Only an admin binds a Slurm cluster, and the cluster list is admin-only:
  // a teacher gets an empty list instead of a 403 that would block the course page.
  // A failed list (a back-end without Slurm yet) must not block the course page either.
  resolve(
    route: ActivatedRouteSnapshot,
    state: RouterStateSnapshot
  ): Observable<Array<ISlurmCluster>> {
    return from(this.permissionsService.hasPermission('ROLE_ADMIN')).pipe(
      switchMap((isAdmin) =>
        isAdmin ? this.slurmClustersApiService.getSlurmClusters() : of([])
      ),
      catchError(() => of([]))
    );
  }
}

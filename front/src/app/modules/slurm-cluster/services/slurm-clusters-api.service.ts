import { Inject, Injectable } from '@angular/core';
import { APP_CONFIG, IAppConfig } from '../../../app-config';
import { HttpClient } from '@angular/common/http';
import {
  ISlurmCluster,
  ISlurmClusterUpdateDto,
} from '../interfaces/slurm-cluster';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class SlurmClustersApiService {
  constructor(
    @Inject(APP_CONFIG) private readonly config: IAppConfig,
    private readonly httClient: HttpClient
  ) {}

  getSlurmClusters(): Observable<Array<ISlurmCluster>> {
    return this.httClient.get<Array<ISlurmCluster>>(
      `${this.config.back_url}admin/slurm-clusters/`
    );
  }

  // Admin management list: id + displayName + technicalName + apiEndpoint,
  // never the secret. Distinct from getSlurmClusters (the teacher dropdown).
  getSlurmClusterDetails(): Observable<Array<ISlurmCluster>> {
    return this.httClient.get<Array<ISlurmCluster>>(
      `${this.config.back_url}admin/slurm-clusters/details`
    );
  }

  getSlurmCluster(id: number): Observable<ISlurmCluster> {
    return this.httClient.get<ISlurmCluster>(
      `${this.config.back_url}admin/slurm-clusters/${id}`
    );
  }

  newSlurmCluster(data: ISlurmClusterUpdateDto): Observable<ISlurmCluster> {
    return this.httClient.post<ISlurmCluster>(
      `${this.config.back_url}admin/slurm-clusters`,
      data
    );
  }

  editSlurmCluster(
    id: number,
    data: ISlurmClusterUpdateDto
  ): Observable<ISlurmCluster> {
    return this.httClient.put<ISlurmCluster>(
      `${this.config.back_url}admin/slurm-clusters/${id}`,
      data
    );
  }

  deleteSlurmCluster(id: number): Observable<void> {
    return this.httClient.delete<void>(
      `${this.config.back_url}admin/slurm-clusters/${id}`
    );
  }
}

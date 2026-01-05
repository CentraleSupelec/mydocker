import { Inject, Injectable } from '@angular/core';
import { APP_CONFIG, IAppConfig } from "../../../app-config";
import { HttpClient } from "@angular/common/http";
import { ICluster, IClusterBasicInformaion, IClusterUpdateDto } from "../interfaces/cluster";
import { Observable } from "rxjs";

@Injectable({
  providedIn: 'root'
})
export class ClustersApiService {
  constructor(
    @Inject(APP_CONFIG) private readonly config: IAppConfig,
    private readonly httClient: HttpClient,
  ) {
  }

  getClustersBasicInformation(): Observable<Array<IClusterBasicInformaion>> {
    return this.httClient.get<Array<IClusterBasicInformaion>>(`${this.config.back_url}admin/clusters/partial_information`);
  }

  getClusters(): Observable<Array<ICluster>> {
    return this.httClient.get<Array<ICluster>>(`${this.config.back_url}admin/clusters/`);
  }

  getCluster(id: number): Observable<ICluster> {
    return this.httClient.get<ICluster>(`${this.config.back_url}admin/clusters/${id}`);
  }

  newCluster(data: IClusterUpdateDto): Observable<ICluster> {
    return this.httClient.post<ICluster>(`${this.config.back_url}admin/clusters`, data);
  }

  editCluster(id: number, data: IClusterUpdateDto): Observable<ICluster> {
    return this.httClient.put<ICluster>(`${this.config.back_url}admin/clusters/${id}`, data);
  }

  deleteCluster(id: number): Observable<void> {
    return this.httClient.delete<void>(`${this.config.back_url}admin/clusters/${id}`);
  }
}

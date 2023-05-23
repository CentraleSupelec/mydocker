import { Inject, Injectable } from '@angular/core';
import { APP_CONFIG, IAppConfig } from "../../../app-config";
import { HttpClient } from "@angular/common/http";
import { IComputeType, IComputeTypeUpdateDto } from "../interfaces/compute-type";
import { Observable } from "rxjs";

@Injectable({
  providedIn: 'root'
})
export class ComputeTypesApiService {
  constructor(
    @Inject(APP_CONFIG) private readonly config: IAppConfig,
    private readonly httClient: HttpClient,
  ) {
  }

  getComputeTypes(): Observable<Array<IComputeType>> {
    return this.httClient.get<Array<IComputeType>>(`${this.config.back_url}admin/compute-types/`);
  }

  getComputeType(id: number): Observable<IComputeType> {
    return this.httClient.get<IComputeType>(`${this.config.back_url}admin/compute-types/${id}`);
  }

  newComputeType(data: IComputeTypeUpdateDto): Observable<IComputeType> {
    return this.httClient.post<IComputeType>(`${this.config.back_url}admin/compute-types`, data);
  }

  editComputeType(id: number, data: IComputeTypeUpdateDto): Observable<IComputeType> {
    return this.httClient.put<IComputeType>(`${this.config.back_url}admin/compute-types/${id}`, data);
  }

  deleteComputeType(id: number): Observable<void> {
    return this.httClient.delete<void>(`${this.config.back_url}admin/compute-types/${id}`);
  }
}

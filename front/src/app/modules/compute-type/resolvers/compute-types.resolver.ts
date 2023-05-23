import { Injectable } from '@angular/core';
import {
  Resolve,
  RouterStateSnapshot,
  ActivatedRouteSnapshot
} from '@angular/router';
import { Observable } from 'rxjs';
import { IComputeType } from '../interfaces/compute-type';
import { ComputeTypesApiService } from '../services/compute-types-api.service';

@Injectable({
  providedIn: 'root'
})
export class ComputeTypesResolver implements Resolve<Array<IComputeType>> {
  constructor(private readonly computeTypesApiService: ComputeTypesApiService) {
  }

  resolve(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<Array<IComputeType>> {
    return this.computeTypesApiService.getComputeTypes();
  }
}

import { Injectable } from '@angular/core';
import {
  Resolve,
  RouterStateSnapshot,
  ActivatedRouteSnapshot
} from '@angular/router';
import { Observable } from 'rxjs';
import { ComputeTypesApiService } from '../services/compute-types-api.service';
import { IComputeType } from '../interfaces/compute-type';

@Injectable({
  providedIn: 'root'
})
export class ComputeTypeResolver implements Resolve<IComputeType> {
  constructor(private readonly computeTypesApiService: ComputeTypesApiService) {
  }
  resolve(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<IComputeType> {
    const id = parseInt(<string>route.paramMap.get('id'), 10);
    return this.computeTypesApiService.getComputeType(id);
  }
}

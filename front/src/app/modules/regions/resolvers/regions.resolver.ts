import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, Resolve, RouterStateSnapshot } from '@angular/router';
import { Observable } from 'rxjs';
import { RegionApiService } from "../services/region-api.service";

@Injectable({
  providedIn: 'root'
})
export class RegionsResolver implements Resolve<string[]> {
  constructor(
    private readonly regionApiService: RegionApiService,
  ) {
  }
  resolve(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<string[]> {
    return this.regionApiService.getAvailableRegion();
  }
}

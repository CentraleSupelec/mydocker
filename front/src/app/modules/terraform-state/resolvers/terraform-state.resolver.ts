import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, Resolve, RouterStateSnapshot } from '@angular/router';
import { Observable } from 'rxjs';
import { TerraformStateApiServiceService } from "../services/terraform-state-api-service.service";
import { ITerraformInstance } from "../interfaces/terraform-instance";

@Injectable({
  providedIn: 'root'
})
export class TerraformStateResolver implements Resolve<ITerraformInstance[]> {
  constructor(
    private readonly terraformStateApiService: TerraformStateApiServiceService,
  ) {
  }

  resolve(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<ITerraformInstance[]> {
    return this.terraformStateApiService.getTerraformState();
  }
}

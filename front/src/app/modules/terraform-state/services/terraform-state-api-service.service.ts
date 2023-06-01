import { Inject, Injectable } from '@angular/core';
import { APP_CONFIG, IAppConfig } from "../../../app-config";
import { HttpClient } from "@angular/common/http";
import { Observable } from "rxjs";
import { ITerraformInstance } from "../interfaces/terraform-instance";

@Injectable({
  providedIn: 'root'
})
export class TerraformStateApiServiceService {
  constructor(
    @Inject(APP_CONFIG) private readonly config: IAppConfig,
    private readonly httClient: HttpClient,
  ) {}

  getTerraformState(): Observable<ITerraformInstance[]> {
    return this.httClient.get<ITerraformInstance[]>(`${this.config.back_url}admin/scale/terraform-state`);
  }
}

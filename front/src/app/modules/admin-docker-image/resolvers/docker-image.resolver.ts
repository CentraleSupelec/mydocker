import { Injectable } from '@angular/core';
import {
  Router, Resolve,
  RouterStateSnapshot,
  ActivatedRouteSnapshot
} from '@angular/router';
import { Observable, of } from 'rxjs';
import { IDockerImage } from "../interfaces/docker-image";
import { DockerImageApiService } from "../services/docker-image-api.service";

@Injectable({
  providedIn: 'root'
})
export class DockerImageResolver implements Resolve<IDockerImage> {
  constructor(
    private readonly dockerImageApiService: DockerImageApiService,
  ) {
  }

  resolve(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<IDockerImage> {
    const id = parseInt(<string>route.paramMap.get('id'), 10);
    return this.dockerImageApiService.getDockerImage(id);
  }
}

import { Injectable } from '@angular/core';
import {
  Resolve,
  RouterStateSnapshot,
  ActivatedRouteSnapshot
} from '@angular/router';
import { Observable } from 'rxjs';
import { IContent } from '../interfaces/content';
import { ContentsApiService } from '../services/contents-api.service';

@Injectable({
  providedIn: 'root'
})
export class ContentsResolver implements Resolve<Array<IContent>> {
  constructor(private readonly contentsApiService: ContentsApiService) {
  }

  resolve(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<Array<IContent>> {
    return this.contentsApiService.getContents();
  }
}

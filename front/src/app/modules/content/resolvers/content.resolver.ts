import { Injectable } from '@angular/core';
import {
  Resolve,
  RouterStateSnapshot,
  ActivatedRouteSnapshot
} from '@angular/router';
import { Observable } from 'rxjs';
import { ContentsApiService } from '../services/contents-api.service';
import { IContent } from '../interfaces/content';

@Injectable({
  providedIn: 'root'
})
export class ContentResolver implements Resolve<IContent> {
  constructor(private readonly contentsApiService: ContentsApiService) {
  }
  resolve(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<IContent> {
    const id = parseInt(<string>route.paramMap.get('id'), 10);
    return this.contentsApiService.getContent(id);
  }
}

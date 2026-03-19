import { Injectable } from '@angular/core';
import {
  Resolve,
  RouterStateSnapshot,
  ActivatedRouteSnapshot
} from '@angular/router';
import { Observable } from 'rxjs';
import { ContentsApiService } from '../services/contents-api.service';
import { IContent } from '../../content/interfaces/content';

@Injectable({
  providedIn: 'root'
})
export class ContentResolver implements Resolve<IContent> {
  constructor(private readonly contentsApiService: ContentsApiService) {
  }
  resolve(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<IContent> {
    const slug = <string>route.paramMap.get('slug');
    return this.contentsApiService.getContentBySlug(slug);
  }
}

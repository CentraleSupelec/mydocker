import { Injectable } from '@angular/core';
import {
  Resolve,
  RouterStateSnapshot,
  ActivatedRouteSnapshot
} from '@angular/router';
import { Observable, of } from 'rxjs';
import { ContentsApiService } from '../services/contents-api.service';
import { IContent } from '../../content/interfaces/content';
import { catchError } from 'rxjs/operators';

@Injectable({
  providedIn: 'root'
})
export class ContentResolver implements Resolve<IContent | null> {
  constructor(private readonly contentsApiService: ContentsApiService) {
  }
  resolve(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<IContent | null> {
    const slug = <string>route.paramMap.get('slug');
    return this.contentsApiService.getContentBySlug(slug).pipe(
      catchError((error) => {
        if (error.status === 404) {
          return of(null);
        }

        throw error;
      })
    );
  }
}

import { Inject, Injectable } from '@angular/core';
import { APP_CONFIG, IAppConfig } from "../../../app-config";
import { HttpClient } from "@angular/common/http";
import { Observable } from "rxjs";
import { IContent } from '../../content/interfaces/content';

@Injectable({
  providedIn: 'root'
})
export class ContentsApiService {
  constructor(
    @Inject(APP_CONFIG) private readonly config: IAppConfig,
    private readonly httClient: HttpClient,
  ) {
  }

  getContentBySlug(slug: string): Observable<IContent> {
    return this.httClient.get<IContent>(`${this.config.back_url}content/${slug}`);
  }
}

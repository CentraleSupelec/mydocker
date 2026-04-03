import { Inject, Injectable } from '@angular/core';
import { APP_CONFIG, IAppConfig } from "../../../app-config";
import { HttpClient } from "@angular/common/http";
import { IContent, IContentUpdateDto } from "../interfaces/content";
import { Observable } from "rxjs";

@Injectable({
  providedIn: 'root'
})
export class ContentsApiService {
  constructor(
    @Inject(APP_CONFIG) private readonly config: IAppConfig,
    private readonly httClient: HttpClient,
  ) {
  }

  getContents(): Observable<Array<IContent>> {
    return this.httClient.get<Array<IContent>>(`${this.config.back_url}admin/contents/`);
  }

  getContent(id: number): Observable<IContent> {
    return this.httClient.get<IContent>(`${this.config.back_url}admin/contents/${id}`);
  }

  newContent(data: IContentUpdateDto): Observable<IContent> {
    return this.httClient.post<IContent>(`${this.config.back_url}admin/contents`, data);
  }

  editContent(id: number, data: IContentUpdateDto): Observable<IContent> {
    return this.httClient.put<IContent>(`${this.config.back_url}admin/contents/${id}`, data);
  }

  deleteContent(id: number): Observable<void> {
    return this.httClient.delete<void>(`${this.config.back_url}admin/contents/${id}`);
  }

  getContentBySlug(slug: string): Observable<IContent> {
    return this.httClient.get<IContent>(`${this.config.back_url}content/${slug}`);
  }
}

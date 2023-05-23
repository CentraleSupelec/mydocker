import { Inject, Injectable } from '@angular/core';
import { APP_CONFIG, IAppConfig } from "../../../app-config";
import { HttpClient, HttpParams } from "@angular/common/http";
import { Observable } from "rxjs";
import { ICreationDockerImage, IDockerImage, IUpdateDockerImage } from "../interfaces/docker-image";
import { IDockerImageBuild } from "../interfaces/docker-image-build";
import { IPort } from "../../ports-form/interfaces/port";
import { IContainer } from "../../shell/interfaces/container";
import { IPageResponse } from "../../utils/page";

@Injectable({
  providedIn: 'root'
})
export class DockerImageApiService {

  constructor(
    @Inject(APP_CONFIG) private readonly config: IAppConfig,
    private readonly httClient: HttpClient,
  ) { }

  getDockerImages(
    query: string,
    page: number | undefined,
    limit: number | undefined,
    sort: string | undefined,
    direction: 'asc' | 'desc' | undefined,
  ): Observable<IPageResponse<IDockerImage>> {
    let params = new HttpParams()
      .append('size', limit ? limit.toString() : '')
      .append('page', page ? page.toString(): '')
      .append('search', query);

    if (sort && direction) {
      params = params.append('sort', `${sort},${direction}`);
    }

    return this.httClient.get<IPageResponse<IDockerImage>>(`${this.config.back_url}images/`, {
      params: params
    });
  }

  getDockerImage(id: number): Observable<IDockerImage> {
    return this.httClient.get<IDockerImage>(`${this.config.back_url}images/${id}`)
  }

  getContext(id: number): Observable<Blob> {
    return this.httClient.get(`${this.config.back_url}images/context/${id}`, {responseType: 'blob'})
  }

  updateDockerImage(id: number, image: IUpdateDockerImage): Observable<IDockerImage> {
    const formData: FormData = this.createFormData(image);
    return this.httClient.put<IDockerImage>(`${this.config.back_url}images/${id}`, formData)
  }

  createDockerImage(image: ICreationDockerImage): Observable<IDockerImage> {
    const formData: FormData = this.createFormData(image);
    formData.append('name', image.name);
    return this.httClient.post<IDockerImage>(`${this.config.back_url}images/`, formData)
  }

  getDockerImageBuild(id: number): Observable<IDockerImageBuild[]> {
    return this.httClient.get<IDockerImageBuild[]>(`${this.config.back_url}images/${id}/build`)
  }

  buildDockerImage(id: number): Observable<void> {
    return this.httClient.post<void>(`${this.config.back_url}images/${id}/build`, {});
  }

  initTestDockerImageBuild(id: number): Observable<void> {
    return this.httClient.post<void>(`${this.config.back_url}images/build/${id}`, {})
  }

  getTestDockerImageBuild(id: number): Observable<IContainer> {
    return this.httClient.get<IContainer>(`${this.config.back_url}images/build/${id}`)
  }

  getLogs(id: number): Observable<string> {
    return this.httClient.get(`${this.config.back_url}images/build/logs/${id}`, {
      observe: 'body',
      responseType: 'text'
    })
  }

  private createFormData(image: IUpdateDockerImage): FormData {
    const formData: FormData = new FormData();
    formData.append('description', image.description)
    formData.append('dockerFile', image.dockerFile)
    formData.append('wrapperScript', image.wrapperScript)
    formData.append('visible', JSON.stringify(image.visible))
    image.ports.forEach((port: IPort, index: number) => {
      Object.keys(port).forEach((key) => {
        // @ts-ignore
        formData.append(`ports[${index}].${key}`, port[key])
      })
    })
    if(image.contextFolder !== null) {
      formData.append('contextFolder', image.contextFolder)
    }
    return formData;
  }
}


import { Component, OnInit, ViewChild } from '@angular/core';
import { IDockerImage } from "../../interfaces/docker-image";
import { DockerImageApiService } from "../../services/docker-image-api.service";
import { MatPaginator } from "@angular/material/paginator";
import { MatSort } from "@angular/material/sort";
import { merge, Subject } from "rxjs";
import { debounceTime, tap } from "rxjs/operators";
import { mergeMap } from "rxjs/operators";

@Component({
  selector: 'app-docker-image-list',
  templateUrl: './docker-image-list.component.html',
  styleUrls: ['./docker-image-list.component.css']
})
export class DockerImageListComponent implements OnInit {
  images: IDockerImage[] = [];
  imagesSize: number | undefined;
  loading = true;
  displayedColumns = ['status', 'name', 'creator', 'createdOn', 'description', 'action'];

  query: string = '';

  @ViewChild(MatPaginator, {static: true}) paginator: MatPaginator | undefined;
  @ViewChild(MatSort, {static: true}) sort: MatSort | null = null;

  readonly searchChange$: Subject<void> = new Subject<void>();

  constructor(
    private readonly dockerImageApiService: DockerImageApiService,
  ) { }

  ngOnInit(): void {
    if (this.paginator && this.sort) {
      merge(
        this.paginator.page,
        this.sort.sortChange.pipe(
          tap(() => {
            if (this.paginator) {
              this.paginator.pageIndex = 0;
            }
          })
        ),
        this.searchChange$
          .pipe(
            debounceTime(150),
            tap(() => {
              if (this.paginator) {
                this.paginator.pageIndex = 0;
              }
            }),
          ),
      ).pipe(
        mergeMap(() => {
          this.loading = true;
          return this.dockerImageApiService.getDockerImages(
            this.query,
            this.paginator?.pageIndex,
            this.paginator?.pageSize,
            this.sort?.active,
            this.sort?.direction || 'asc'
          )
        })
      ).subscribe(
        page => {
          this.loading = false;
          this.images = page.content
          this.imagesSize = page.totalElements
        }
      )
      this.paginator.pageSize = 25
      this.paginator.pageIndex = 0
      this.sort.active = 'createdOn'
      this.sort.direction = 'desc'
    }
    this.searchChange$.next();
  }

  updateQuery() {
    this.searchChange$.next();
  }

  clearQuery() {
    this.query = '';
    this.searchChange$.next();
  }
}

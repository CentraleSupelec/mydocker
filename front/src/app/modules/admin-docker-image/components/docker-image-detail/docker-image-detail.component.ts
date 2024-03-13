import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from "@angular/router";
import { IDockerImage } from "../../interfaces/docker-image";
import { IDockerImageBuild } from "../../interfaces/docker-image-build";
import { mergeMap, takeUntil } from "rxjs/operators";
import { DockerImageApiService } from "../../services/docker-image-api.service";
import { animate, state, style, transition, trigger } from "@angular/animations";
import { interval, Subject } from "rxjs";
import { BuildStatus } from "../../interfaces/build-status";
import { IContainer } from "../../../shell/interfaces/container";
import { MatDialog } from "@angular/material/dialog";
import { DockerImageBuildDetailDialogComponent } from "../docker-image-build-detail-dialog/docker-image-build-detail-dialog.component";
import { DockerImagePermissionDialogComponent } from "../../../permissions/components/docker-image-permission-dialog/docker-image-permission-dialog.component";
import { OpenLogDialogService } from "../../../log-dialog/open-log-dialog.service";

@Component({
  selector: 'app-docker-image-detail',
  templateUrl: './docker-image-detail.component.html',
  styleUrls: ['./docker-image-detail.component.css'],
  animations: [
    trigger('detailExpand', [
      state('collapsed', style({minHeight: '0', height: '0', overflow: 'hidden'})),
      state('expanded', style({height: '*', paddingTop: '15px'})),
      transition('expanded <=> collapsed', animate('225ms cubic-bezier(0.4, 0.0, 0.2, 1)')),
    ]),
  ],
})
export class DockerImageDetailComponent implements OnInit {
  dockerImage: IDockerImage | null = null;
  dockerBuilds: IDockerImageBuild[] = [];
  displayedColumns = ['status', 'buildId', 'updatedOn', 'action'];
  expandedElement: IDockerImageBuild | null = null;
  polling = false;
  loading = true;
  refresh$ = new Subject<void>();
  testContainer: IContainer | null = null;

  private readonly stopPolling$: Subject<void> = new Subject<void>();
  private readonly stopContainerPolling$: Subject<void> = new Subject<void>();

  constructor(
    private readonly activatedRoute: ActivatedRoute,
    private readonly dockerImageApiService: DockerImageApiService,
    private readonly matDialog: MatDialog,
    private readonly openLogsDialogService: OpenLogDialogService,
  ) { }

  ngOnInit(): void {
    this.refresh$.pipe(
      mergeMap(() => {
        this.loading = true;
        return this.dockerImageApiService.getDockerImageBuild(<number>this.dockerImage?.id)
      })
    ).subscribe(
      dockerBuilds => {
        this.loading = false;
        this.dockerBuilds = dockerBuilds
        if (dockerBuilds[0]?.status !== BuildStatus.BUILDING) {
          this.stopPolling$.next()
          this.polling = false;
        }
      }
    )

    this.activatedRoute.data
      .subscribe(data => {
        this.dockerImage = data.docker_image
        this.startPolling()
    });
  }

  createNewBuild() {
    this.dockerImageApiService.buildDockerImage(<number>this.dockerImage?.id)
      .subscribe(
        () => this.startPolling()
      )
  }

  private startPolling() {
    this.polling = true;
    interval(3000).pipe(
      takeUntil(this.stopPolling$),
    ).subscribe(
      () => this.refresh$.next()
    )
  }

  testBuild(element: IDockerImageBuild) {
    // stop precedent polling if exist
    this.stopContainerPolling$.next();
    this.testContainer = null;
    this.expandedElement = this.expandedElement === element ? null : element;
    if (this.expandedElement) {
      this.dockerImageApiService.initTestDockerImageBuild(element.id)
        .pipe(
          mergeMap(() => interval(3000)),
          mergeMap(() => this.dockerImageApiService.getTestDockerImageBuild(element.id)),
          takeUntil(this.stopContainerPolling$),
        ).subscribe(
        container => {
          if(container) {
            this.testContainer = container;
            this.stopContainerPolling$.next();
          }
        }
      )
    }
  }

  openDetailDialog(element: IDockerImageBuild) {
    this.matDialog.open(DockerImageBuildDetailDialogComponent, { data: element});
  }

  openShareDialog() {
    this.matDialog.open(DockerImagePermissionDialogComponent, {
      data: this.dockerImage,
      width: '1000px',
      maxWidth: '60%'
    });
  }

  fetchLogs(element: IDockerImageBuild) {
    this.dockerImageApiService.getLogs(element.id).subscribe(
      logs => this.openLogsDialogService.openDialog(logs)
    );
  }
}

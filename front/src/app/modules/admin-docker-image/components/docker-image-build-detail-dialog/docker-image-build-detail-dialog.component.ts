import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from "@angular/material/dialog";
import { IDockerImageBuild } from "../../interfaces/docker-image-build";

@Component({
  selector: 'app-docker-image-build-detail-dialog',
  templateUrl: './docker-image-build-detail-dialog.component.html',
  styleUrls: ['./docker-image-build-detail-dialog.component.css']
})
export class DockerImageBuildDetailDialogComponent {

  constructor(
    @Inject(MAT_DIALOG_DATA) public readonly data: IDockerImageBuild,
    private readonly dialogRef: MatDialogRef<DockerImageBuildDetailDialogComponent>,
  ) {}

  close() {
    this.dialogRef.close();
  }
}

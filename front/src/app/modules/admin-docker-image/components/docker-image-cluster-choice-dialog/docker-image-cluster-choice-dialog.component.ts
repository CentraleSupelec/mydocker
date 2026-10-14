import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from "@angular/material/dialog";
import { IDockerImage } from "../../interfaces/docker-image";
import { map, startWith } from "rxjs/operators";
import { Observable, Subject } from "rxjs";
import { FormBuilder, FormControl } from "@angular/forms";
import { DockerImageApiService } from "../../services/docker-image-api.service";
import { IDockerImageBuild } from "../../interfaces/docker-image-build";
import { mergeMap } from "rxjs/operators";
import { ICluster } from 'src/app/modules/cluster/interfaces/cluster';
import { ClustersApiService } from 'src/app/modules/cluster/services/clusters-api.service';
import { ActivatedRoute } from '@angular/router';

export interface DockerImageClusterChoiceDialogComponentInput {
  clusters: ICluster[];
}

export interface ClusterChoiceDialogReturnData {
  cluster: ICluster;
}

@Component({
  selector: 'app-docker-image-cluster-choice-dialog',
  templateUrl: './docker-image-cluster-choice-dialog.component.html',
  styleUrls: ['./docker-image-cluster-choice-dialog.component.css']
})
export class DockerImageClusterChoiceDialogComponent {
  readonly control: FormControl;

  chosenCluster: ICluster | null = null;

  constructor(
    public dialogRef: MatDialogRef<DockerImageClusterChoiceDialogComponent>,
    formBuilder: FormBuilder,
    @Inject(MAT_DIALOG_DATA) public data: DockerImageClusterChoiceDialogComponentInput,
  ) {
    this.control = formBuilder.control('');
  }

  closeAndReturnCluster() {
    if (this.chosenCluster) {
      const data: ClusterChoiceDialogReturnData = {
        cluster: this.chosenCluster,
      };
      this.dialogRef.close(data);
    }
  }
}

import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from "@angular/material/dialog";
import { IDockerImage } from "../../../admin-docker-image/interfaces/docker-image";
import { map, startWith } from "rxjs/operators";
import { Observable, Subject } from "rxjs";
import { FormBuilder, FormControl } from "@angular/forms";
import { DockerImageApiService } from "../../../admin-docker-image/services/docker-image-api.service";
import { IDockerImageBuild } from "../../../admin-docker-image/interfaces/docker-image-build";
import { mergeMap } from "rxjs/operators";

export interface DockerImageChoiceDialogReturnData {
  dockerImage: IDockerImage;
  dockerImageBuild: IDockerImageBuild;
}

@Component({
  selector: 'app-docker-image-choice-dialog',
  templateUrl: './docker-image-choice-dialog.component.html',
  styleUrls: ['./docker-image-choice-dialog.component.css']
})
export class DockerImageChoiceDialogComponent {
  dockerImages: IDockerImage[] = [];
  dockerImageBuilds: IDockerImageBuild[] = [];
  buildChoiceDisabled = true;
  readonly filteredOptions: Observable<IDockerImage[]>;
  readonly control: FormControl;

  chosenDockerImage: IDockerImage | null = null;
  chosenDockerImageBuild: IDockerImageBuild | null = null;

  constructor(
    public dialogRef: MatDialogRef<DockerImageChoiceDialogComponent>,
    private readonly dockerImageApiService: DockerImageApiService,
    formBuilder: FormBuilder,
  ) {
    this.control = formBuilder.control('');
    this.filteredOptions = this.control.valueChanges
      .pipe(
        startWith(''),
        mergeMap((value: IDockerImage | string) => {
          if (typeof value === 'string') {
            return this._filter(value)
          } else {
            return this._filter(value.name)
          }
        })
      );
  }

  private _filter(value: string): Observable<IDockerImage[]> {
    return this.dockerImageApiService.getDockerImages(value, 0, 10, undefined, undefined)
      .pipe(
        map(v => v.content)
      );
  }

  displayFn(dockerImage: IDockerImage): string {
    return dockerImage.name;
  }

  dockerImageSelected(dockerImage: IDockerImage) {
    this.chosenDockerImage = dockerImage;
    this.dockerImageApiService.getDockerImageBuild(dockerImage.id).subscribe(
      builds => {
        this.dockerImageBuilds = builds;
        this.buildChoiceDisabled = false;
      }
    );
  }

  closeAndReturnDockerImage() {
    if (this.chosenDockerImage && this.chosenDockerImageBuild) {
      const data: DockerImageChoiceDialogReturnData = {
        dockerImage: this.chosenDockerImage,
        dockerImageBuild: this.chosenDockerImageBuild
      };
      this.dialogRef.close(data);
    }
  }
}

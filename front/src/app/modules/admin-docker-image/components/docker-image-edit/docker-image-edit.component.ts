import { Component, OnInit } from '@angular/core';
import { IDockerImage } from "../../interfaces/docker-image";
import { FormBuilder, FormControl } from "@angular/forms";
import { ActivatedRoute } from "@angular/router";
import { ObservableSnackNotificationService } from "../../../utils/snack-notification/observable-snack-notification.service";
import { DockerImageApiService } from "../../services/docker-image-api.service";
import { DockerImagePermissionDialogComponent } from "../../../permissions/components/docker-image-permission-dialog/docker-image-permission-dialog.component";
import { MatDialog } from "@angular/material/dialog";
import { NgxPermissionsService } from "ngx-permissions";

@Component({
  selector: 'app-docker-image-edit',
  templateUrl: './docker-image-edit.component.html',
  styleUrls: ['./docker-image-edit.component.css']
})
export class DockerImageEditComponent implements OnInit {
  dockerImage: IDockerImage | null = null;
  dockerImageForm: FormControl;

  constructor(
    private readonly activatedRoute: ActivatedRoute,
    private readonly formBuilder: FormBuilder,
    private readonly toasterService: ObservableSnackNotificationService,
    private readonly dockerImageApiService: DockerImageApiService,
    private readonly matDialog: MatDialog,
    private readonly permissionService: NgxPermissionsService,
  ) {
    this.dockerImageForm = this.formBuilder.control({});
  }

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(
      data => {
        this.dockerImage = data.docker_image;
        this.dockerImageForm.setValue(this.dockerImage);
        this.permissionService.hasPermission(
          [
            'docker_image.' + this.dockerImage?.id + '.edit',
            'docker_image.' + this.dockerImage?.id + '.creator',
            'ROLE_ADMIN'
          ]
        ).then(
          hasPermission => {
            if (!hasPermission) {
              this.dockerImageForm.disable()
            }
          }
        )
      }
    )
  }

  submit() {
    if (this.dockerImage) {
      this.toasterService.toast(
        this.dockerImageApiService.updateDockerImage(this.dockerImage.id, this.dockerImageForm.value),
        "L'image a bien été mis à jour",
        "Erreur lors de la sauvegarde de l'image",
        true
      )
    }
  }

  openShareDialog() {
    this.matDialog.open(DockerImagePermissionDialogComponent, {
      data: this.dockerImage,
      width: '1000px',
      maxWidth: '60%'
    });
  }

  download() {
    if (this.dockerImage) {
      this.dockerImageApiService.getContext(this.dockerImage.id)
        .subscribe((data: Blob) => {
          const url= window.URL.createObjectURL(data);
          window.open(url);
        })
    }
  }
}

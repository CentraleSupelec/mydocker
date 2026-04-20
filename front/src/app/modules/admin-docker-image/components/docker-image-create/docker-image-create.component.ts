import { Component } from '@angular/core';
import { FormBuilder, FormControl } from "@angular/forms";
import { Router } from "@angular/router";
import { ObservableSnackNotificationService } from "../../../utils/snack-notification/observable-snack-notification.service";
import { DockerImageApiService } from "../../services/docker-image-api.service";
import { NgxPermissionsService } from "ngx-permissions";
import { IDockerImage } from "../../interfaces/docker-image";
import { TranslateService } from '@ngx-translate/core';

@Component({
  selector: 'app-docker-image-create',
  templateUrl: './docker-image-create.component.html',
  styleUrls: ['./docker-image-create.component.css']
})
export class DockerImageCreateComponent {
  readonly dockerImageForm: FormControl;

  constructor(
    formBuilder: FormBuilder,
    private readonly dockerImageApiService: DockerImageApiService,
    private readonly toasterService: ObservableSnackNotificationService,
    private readonly permissionService: NgxPermissionsService,
    private readonly router: Router,
    private readonly translate: TranslateService
  ) {
    this.dockerImageForm = formBuilder.control({});
  }

  submit() {
    this.toasterService.toastWithCallback(
      this.dockerImageApiService.createDockerImage(this.dockerImageForm.value),
      this.translate.instant('admin.docker_images.create_success'),
      this.translate.instant('admin.docker_images.create_error'),
      (result: IDockerImage) => {
        this.permissionService.addPermission('docker_image.' + result.id + '.creator');
        this.router.navigate(['/admin/images']);
      }
    )
  }
}

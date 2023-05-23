import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormControl } from "@angular/forms";
import { AdminCoursesApiService } from "../../services/admin-courses-api.service";
import { ObservableSnackNotificationService } from "../../../utils/snack-notification/observable-snack-notification.service";
import { NgxPermissionsService } from "ngx-permissions";
import { ActivatedRoute, Router } from "@angular/router";
import { IAdminCourse } from "../../interfaces/course";
import { Location } from '@angular/common';
import { IComputeType } from '../../../compute-type/interfaces/compute-type';

@Component({
  selector: 'app-course-new',
  templateUrl: './course-new.component.html',
  styleUrls: ['./course-new.component.css']
})
export class CourseNewComponent implements OnInit {
  readonly courseForm: FormControl;
  computeTypes?: IComputeType[];
  constructor(
    formBuilder: FormBuilder,
    private readonly adminCoursesApi: AdminCoursesApiService,
    private readonly toasterService: ObservableSnackNotificationService,
    private readonly permissionService: NgxPermissionsService,
    private readonly router: Router,
    private readonly location: Location,
    private readonly route: ActivatedRoute
  ) {
    this.courseForm = formBuilder.control({});
  }

  ngOnInit(): void {
    this.route.data.subscribe(
      d => {
        this.computeTypes = d.computeTypes;
      }
    )
  }

  submit() {
    this.toasterService.toastWithCallback(
      this.adminCoursesApi.createCourse(this.courseForm.value),
      "Le cours a bien été crée",
      "Erreur lors de la création du cours",
      (result: IAdminCourse) => {
        this.permissionService.addPermission('course.' + result.id + '.creator');
        this.location.back();
      }
    )
  }
}

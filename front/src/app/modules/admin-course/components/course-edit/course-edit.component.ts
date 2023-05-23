import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { IAdminCourse } from "../../interfaces/course";
import { ActivatedRoute } from "@angular/router";
import { FormBuilder, FormControl } from "@angular/forms";
import { AdminCoursesApiService } from "../../services/admin-courses-api.service";
import { ObservableSnackNotificationService } from "../../../utils/snack-notification/observable-snack-notification.service";
import { CoursePermissionDialogComponent } from "../../../permissions/components/course-permission-dialog/course-permission-dialog.component";
import { MatDialog } from "@angular/material/dialog";
import { CoursePermissionService } from "../../services/course-permission.service";
import { IComputeType } from '../../../compute-type/interfaces/compute-type';
import { ISessionsById } from "../../../sessions-form/interfaces/admin-session";

@Component({
  selector: 'app-course-edit',
  templateUrl: './course-edit.component.html',
  styleUrls: ['./course-edit.component.css']
})
export class CourseEditComponent implements OnInit {
  course: IAdminCourse | undefined;
  computeTypes?: Array<IComputeType>;
  readonly courseForm: FormControl;
  sessionsById: ISessionsById = {};

  constructor(
    private readonly route: ActivatedRoute,
    private readonly formBuilder: FormBuilder,
    private readonly adminCoursesApi: AdminCoursesApiService,
    private readonly toasterService: ObservableSnackNotificationService,
    private readonly matDialog: MatDialog,
    private readonly coursePermissionService: CoursePermissionService,
    private readonly cd: ChangeDetectorRef
  ) {
    this.courseForm = formBuilder.control({});
  }

  ngOnInit(): void {
    this.route.data.subscribe(
      d => {
        this.course = d.course;
        this.sessionsById = {};
        this.course?.sessions.forEach( session => {
          this.sessionsById[session.id] = session;
        });
        this.computeTypes = d.computeTypes;
        this.courseForm.setValue(this.course)
        this.coursePermissionService.hasEditPermission(this.course?.id).then(
          hasPermission => {
            if (!hasPermission) {
              this.courseForm.disable();
            }
          }
        )
        this.courseForm.statusChanges.subscribe(() => {
          this.cd.detectChanges();
        })
      }
    )
  }

  submit() {
    if (this.course) {
      this.toasterService.toast(
        this.adminCoursesApi.updateCourse(this.course.id, this.courseForm.value),
        "Le cours a bien été mis à jour",
        "Erreur lors de la sauvegarde du cours",
        false,
        ['/admin/courses']
      )
    }
  }

  openShareDialog() {
    this.matDialog.open(CoursePermissionDialogComponent, {
      data: this.course,
      width: '1000px',
      maxWidth: '60%'
    });
  }
}

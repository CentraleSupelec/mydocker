import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router } from "@angular/router";
import { defer, of } from "rxjs";

import { CourseEditComponent } from './course-edit.component';
import { AdminCourseModule } from "../../admin-course.module";
import { RouterTestingModule } from "@angular/router/testing";
import { HttpClientTestingModule, HttpTestingController } from "@angular/common/http/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { NgxPermissionsModule, NgxPermissionsService } from "ngx-permissions";
import { TranslateTestingModule } from 'src/testing/translate-testing.module';
import { APP_CONFIG } from "../../../../app-config";
import { ConfirmDialogService } from "../../../utils/confirm-dialog/confirm-dialog.service";
import { CourseStatus, IAdminCourse } from "../../interfaces/course";

const COURSE = {
  id: 42,
  uuid: 'course-uuid',
  title: 'Recette',
  status: CourseStatus.READY,
  sessions: [],
} as unknown as IAdminCourse;

describe('CourseEditComponent', () => {
  let component: CourseEditComponent;
  let fixture: ComponentFixture<CourseEditComponent>;
  let routeData: { course: IAdminCourse };
  let confirmDialog: jasmine.SpyObj<ConfirmDialogService>;
  let httpTestingController: HttpTestingController;

  beforeEach(async () => {
    routeData = {course: COURSE};
    confirmDialog = jasmine.createSpyObj<ConfirmDialogService>('ConfirmDialogService', ['confirm']);
    await TestBed.configureTestingModule({
      declarations: [ CourseEditComponent ],
      imports: [
        TranslateTestingModule,
        AdminCourseModule,
        RouterTestingModule,
        HttpClientTestingModule,
        NoopAnimationsModule,
        NgxPermissionsModule.forRoot(),
      ],
      providers: [
        { provide: ActivatedRoute, useValue: { data: defer(() => of(routeData)) } },
        { provide: APP_CONFIG, useValue: { back_url: 'http://back/' } },
        { provide: ConfirmDialogService, useValue: confirmDialog },
      ],
    })
    .compileComponents();
    httpTestingController = TestBed.inject(HttpTestingController);
  });

  async function render(permissions: string[]) {
    const permissionsService = TestBed.inject(NgxPermissionsService);
    permissionsService.flushPermissions();
    permissionsService.loadPermissions(permissions);
    fixture = TestBed.createComponent(CourseEditComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  }

  function deleteButton(): HTMLButtonElement | undefined {
    const buttons: HTMLButtonElement[] = Array.from(fixture.nativeElement.querySelectorAll('button'));
    return buttons.find(b => b.textContent?.trim() === 'admin.courses.edit.delete.button');
  }

  it('should create', async () => {
    await render([]);
    expect(component).toBeTruthy();
  });

  it('shows the Delete button to whoever may edit the course', async () => {
    for (const permissions of [['course.42.edit'], ['course.42.creator'], ['ROLE_ADMIN']]) {
      await render(permissions);
      expect(deleteButton()).withContext(permissions.join()).toBeTruthy();
      fixture.destroy();
    }
  });

  it('hides the Delete button from a reader and on a deleted course', async () => {
    await render(['course.42.view']);
    expect(deleteButton()).toBeUndefined();
    fixture.destroy();

    routeData = {course: {...COURSE, status: CourseStatus.DELETED}};
    await render(['ROLE_ADMIN']);
    expect(deleteButton()).toBeUndefined();
  });

  it('cancel in the modal sends nothing', async () => {
    confirmDialog.confirm.and.returnValue(of(false));
    await render(['course.42.edit']);

    deleteButton()!.click();
    fixture.detectChanges();

    expect(confirmDialog.confirm).toHaveBeenCalledOnceWith({
      title: 'admin.courses.edit.delete.confirm_title',
      text: 'admin.courses.edit.delete.confirm_text',
    });
    httpTestingController.expectNone('http://back/admin/courses/42/mark-deleted');
    httpTestingController.expectNone({method: 'PUT', url: 'http://back/admin/courses/42'});
    httpTestingController.verify();
  });

  it('confirm sends one PUT mark-deleted', async () => {
    confirmDialog.confirm.and.returnValue(of(true));
    const navigate = spyOn(TestBed.inject(Router), 'navigate').and.resolveTo(true);
    await render(['course.42.edit']);

    deleteButton()!.click();
    fixture.detectChanges();

    const request = httpTestingController.expectOne({method: 'PUT', url: 'http://back/admin/courses/42/mark-deleted'});
    expect(navigate).not.toHaveBeenCalled();
    request.flush({...COURSE, status: CourseStatus.DELETED});

    expect(navigate).toHaveBeenCalledTimes(1);
    expect(navigate.calls.mostRecent().args[0]).toEqual(['/admin/courses']);
    httpTestingController.verify();
  });
});

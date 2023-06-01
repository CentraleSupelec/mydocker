import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CourseGeneralInformationFormComponent } from './course-general-information-form.component';
import { AdminCourseModule } from "../../admin-course.module";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { NgxPermissionsModule } from "ngx-permissions";

describe('CourseGeneralInformationFormComponent', () => {
  let component: CourseGeneralInformationFormComponent;
  let fixture: ComponentFixture<CourseGeneralInformationFormComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ CourseGeneralInformationFormComponent ],
      imports: [
        AdminCourseModule,
        NoopAnimationsModule,
        NgxPermissionsModule.forRoot(),
      ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(CourseGeneralInformationFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

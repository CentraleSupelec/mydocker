import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CourseNewComponent } from './course-new.component';
import { AdminCourseModule } from "../../admin-course.module";
import { HttpClientTestingModule } from "@angular/common/http/testing";
import { RouterTestingModule } from "@angular/router/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { NgxPermissionsModule } from "ngx-permissions";

describe('CourseNewComponent', () => {
  let component: CourseNewComponent;
  let fixture: ComponentFixture<CourseNewComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ CourseNewComponent ],
      imports: [
        AdminCourseModule,
        HttpClientTestingModule,
        RouterTestingModule,
        NoopAnimationsModule,
        NgxPermissionsModule.forRoot(),
      ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(CourseNewComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CourseDisplayFormComponent } from './course-display-form.component';
import { AdminCourseModule } from "../../admin-course.module";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";

describe('CourseDisplayFormComponent', () => {
  let component: CourseDisplayFormComponent;
  let fixture: ComponentFixture<CourseDisplayFormComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ CourseDisplayFormComponent ],
      imports: [
        AdminCourseModule,
        NoopAnimationsModule
      ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(CourseDisplayFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

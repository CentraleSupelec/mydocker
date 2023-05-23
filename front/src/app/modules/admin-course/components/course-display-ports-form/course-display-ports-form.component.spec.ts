import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CourseDisplayPortsFormComponent } from './course-display-ports-form.component';
import { AdminCourseModule } from "../../admin-course.module";

describe('CourseDisplayPortsFormComponent', () => {
  let component: CourseDisplayPortsFormComponent;
  let fixture: ComponentFixture<CourseDisplayPortsFormComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ CourseDisplayPortsFormComponent ],
      imports: [
        AdminCourseModule
      ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(CourseDisplayPortsFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

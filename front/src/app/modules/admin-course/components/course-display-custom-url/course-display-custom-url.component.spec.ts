import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CourseDisplayCustomUrlComponent } from './course-display-custom-url.component';
import { AdminCourseModule } from "../../admin-course.module";

describe('CourseDisplayCustomUrlComponent', () => {
  let component: CourseDisplayCustomUrlComponent;
  let fixture: ComponentFixture<CourseDisplayCustomUrlComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ CourseDisplayCustomUrlComponent ],
      imports: [
        AdminCourseModule
      ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(CourseDisplayCustomUrlComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

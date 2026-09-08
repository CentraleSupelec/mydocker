import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CourseDisplayUrlComponent } from './course-display-url.component';
import { AdminCourseModule } from "../../admin-course.module";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateTestingModule } from 'src/testing/translate-testing.module';

describe('CourseDisplayUrlComponent', () => {
  let component: CourseDisplayUrlComponent;
  let fixture: ComponentFixture<CourseDisplayUrlComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ CourseDisplayUrlComponent ],
      imports: [
        TranslateTestingModule,
        AdminCourseModule,
        NoopAnimationsModule
      ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(CourseDisplayUrlComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

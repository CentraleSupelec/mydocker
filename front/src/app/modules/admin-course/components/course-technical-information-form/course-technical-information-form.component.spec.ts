import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CourseTechnicalInformationFormComponent } from './course-technical-information-form.component';
import { AdminCourseModule } from "../../admin-course.module";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { HttpClientTestingModule } from "@angular/common/http/testing";

describe('CourseTechnicalInformationFormComponent', () => {
  let component: CourseTechnicalInformationFormComponent;
  let fixture: ComponentFixture<CourseTechnicalInformationFormComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ CourseTechnicalInformationFormComponent ],
      imports: [
        AdminCourseModule,
        NoopAnimationsModule,
        HttpClientTestingModule,
      ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(CourseTechnicalInformationFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

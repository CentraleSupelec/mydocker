import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CourseNewComponent } from './course-new.component';
import { AdminCourseModule } from "../../admin-course.module";
import { HttpClientTestingModule } from "@angular/common/http/testing";
import { RouterTestingModule } from "@angular/router/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { NgxPermissionsModule } from "ngx-permissions";
import { TranslateTestingModule } from 'src/testing/translate-testing.module';
import { ActivatedRoute } from '@angular/router';
import { By } from '@angular/platform-browser';
import { of } from 'rxjs';
import { CourseFormComponent } from '../course-form/course-form.component';
import { ISlurmCluster } from '../../../slurm-cluster/interfaces/slurm-cluster';

describe('CourseNewComponent', () => {
  let component: CourseNewComponent;
  let fixture: ComponentFixture<CourseNewComponent>;
  const slurmClusters: ISlurmCluster[] = [{
    id: 1,
    displayName: 'Test cluster',
    technicalName: 'test',
    apiEndpoint: 'http://localhost:8081',
    secretKey: '',
  }];

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ CourseNewComponent ],
      imports: [
        TranslateTestingModule,
        AdminCourseModule,
        HttpClientTestingModule,
        RouterTestingModule,
        NoopAnimationsModule,
        NgxPermissionsModule.forRoot(),
      ],
      providers: [
        {
          provide: ActivatedRoute,
          useValue: { data: of({ computeTypes: [], slurmClusters }) },
        },
      ],
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

  it('should pass the resolved Slurm clusters to the course form', () => {
    const courseForm = fixture.debugElement.query(By.directive(CourseFormComponent))
      .componentInstance as CourseFormComponent;

    expect(component.slurmClusters).toEqual(slurmClusters);
    expect(courseForm.slurmClusters).toEqual(slurmClusters);
  });
});

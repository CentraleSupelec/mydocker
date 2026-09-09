import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CourseListComponent } from './course-list.component';
import { RouterTestingModule } from "@angular/router/testing";
import { ActivatedRoute, convertToParamMap, Router } from "@angular/router";
import { of } from "rxjs";
import { MatExpansionModule } from "@angular/material/expansion";
import { APP_CONFIG } from 'src/app/app-config';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TranslateTestingModule } from 'src/testing/translate-testing.module';

describe('CourseListComponent', () => {
  let component: CourseListComponent;
  let fixture: ComponentFixture<CourseListComponent>;

  const sessions = of({ sessions: [] });
  const queryParamMap = of(convertToParamMap({}));

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [CourseListComponent],
      imports: [
        TranslateTestingModule,
        RouterTestingModule,
        HttpClientTestingModule,
        MatExpansionModule,
      ],
      providers: [{
        provide: ActivatedRoute,
        useValue: {
          data: sessions,
          queryParamMap: queryParamMap
        }
      },
      {
        provide: APP_CONFIG,
        useValue: {}
      }
      ]
    })
      .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(CourseListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

describe('CourseListComponent, consuming the launch parameter', () => {
  let component: CourseListComponent;
  let fixture: ComponentFixture<CourseListComponent>;
  let router: Router;
  let httpMock: HttpTestingController;

  const tomorrow = new Date().getTime() + 24 * 3600 * 1000;
  const courses = [{
    id: 42,
    title: 'Course 42',
    sessions: [{ id: 7, startDateTime: tomorrow }],
  }];

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [CourseListComponent],
      imports: [
        TranslateTestingModule,
        RouterTestingModule,
        HttpClientTestingModule,
        MatExpansionModule,
      ],
      providers: [
        {
          provide: ActivatedRoute,
          useValue: {
            data: of({ courses }),
            queryParamMap: of(convertToParamMap({
              course_id: '42',
              session_id: '7',
              user_redirect: '/git_clone?repo=lab',
            }))
          }
        },
        {
          provide: APP_CONFIG,
          useValue: { back_url: 'http://back/', polling_interval_in_milliseconds: 60000 }
        }
      ]
    })
      .compileComponents();

    router = TestBed.inject(Router);
    httpMock = TestBed.inject(HttpTestingController);
  });

  function loadDashboard(): void {
    fixture = TestBed.createComponent(CourseListComponent);
    component = fixture.componentInstance;
    // ngOnInit rather than detectChanges: none of this is about the template.
    component.ngOnInit();
  }

  it('strips course_id once consumed and keeps session_id and user_redirect', () => {
    const navigate = spyOn(router, 'navigate');

    loadDashboard();

    expect(component.courseId).toBe(42);
    expect(navigate).toHaveBeenCalledTimes(1);
    expect(navigate).toHaveBeenCalledWith([], {
      queryParams: {
        session_id: '7',
        user_redirect: '/git_clone?repo=lab',
      },
      replaceUrl: true
    });
  });

  it('carries a start intent for the course the link named', () => {
    loadDashboard();

    httpMock.expectOne('http://back/courses/42/isGpu').flush(false);

    expect(component.launchSessionId).toBe(7);
    expect(component.intentFor(component.planified[0])).toBe('start');
  });

  it('carries no start intent for a GPU course, an exception this change preserves', () => {
    loadDashboard();

    httpMock.expectOne('http://back/courses/42/isGpu').flush(true);

    expect(component.launchSessionId).toBeUndefined();
    expect(component.intentFor(component.planified[0])).toBe('none');
  });
});

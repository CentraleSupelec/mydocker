import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, Router } from "@angular/router";
import { of } from "rxjs";

import { CourseJoinComponent } from './course-join.component';
import { RouterTestingModule } from "@angular/router/testing";
import { APP_CONFIG } from "../../../../app-config";
import { HttpClientTestingModule, HttpTestingController } from "@angular/common/http/testing";
import { MatProgressSpinnerModule } from "@angular/material/progress-spinner";
import { TranslateTestingModule } from 'src/testing/translate-testing.module';

describe('CourseJoinComponent', () => {
  let component: CourseJoinComponent;
  let fixture: ComponentFixture<CourseJoinComponent>;
  let httpTestingController: HttpTestingController;
  let navigate: jasmine.Spy;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ CourseJoinComponent ],
      imports: [
        TranslateTestingModule,
        RouterTestingModule,
        HttpClientTestingModule,
        MatProgressSpinnerModule,
      ],
      providers: [
        {
          provide: APP_CONFIG,
          useValue: {back_url: 'http://back/'}
        },
        {
          provide: ActivatedRoute,
          useValue: {paramMap: of(convertToParamMap({link: 'recetteLink'}))}
        },
      ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    httpTestingController = TestBed.inject(HttpTestingController);
    navigate = spyOn(TestBed.inject(Router), 'navigate').and.resolveTo(true);
    fixture = TestBed.createComponent(CourseJoinComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  function answerLookup(body: any, status = 200, statusText = 'OK') {
    httpTestingController
      .expectOne({method: 'GET', url: 'http://back/courses/recetteLink'})
      .flush(body, {status, statusText});
  }

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('a deleted course\'s link shows the no-longer-available message and does not join', () => {
    answerLookup(null, 410, 'Gone');

    expect(navigate).toHaveBeenCalledWith(['/'], {
      queryParams: {error_message: 'container.join_course_deleted'},
      replaceUrl: true
    });
    httpTestingController.expectNone({method: 'PUT', url: 'http://back/courses/42/join'});
    httpTestingController.verify();
  });

  it('a refused join shows the same message', () => {
    answerLookup({id: 42});
    httpTestingController
      .expectOne({method: 'PUT', url: 'http://back/courses/42/join'})
      .flush(null, {status: 410, statusText: 'Gone'});

    expect(navigate).toHaveBeenCalledWith(['/'], {
      queryParams: {error_message: 'container.join_course_deleted'},
      replaceUrl: true
    });
    httpTestingController.verify();
  });

  it('an unknown link still shows join_error', () => {
    answerLookup(null, 404, 'Not Found');

    expect(navigate).toHaveBeenCalledWith(['/'], {
      queryParams: {error_message: 'container.join_error'},
      replaceUrl: true
    });
  });
});

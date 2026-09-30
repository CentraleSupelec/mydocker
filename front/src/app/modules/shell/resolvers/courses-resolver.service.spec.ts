import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { throwError, of } from 'rxjs';
import { UserCourseApiService } from '../services/user-course-api.service';
import { CourseSessionsResolver } from './courses-resolver.service';
import { UserCourseSessionsResolver } from './user-courses-resolver.service';

describe('course list resolvers', () => {
  const route = {} as ActivatedRouteSnapshot;
  const state = {} as RouterStateSnapshot;
  let api: jasmine.SpyObj<UserCourseApiService>;

  beforeEach(() => {
    api = jasmine.createSpyObj('UserCourseApiService', ['getUserCourses', 'getUserCourseSessions']);
    TestBed.configureTestingModule({ providers: [{ provide: UserCourseApiService, useValue: api }] });
  });

  it('CourseSessionsResolver passes a success through', (done) => {
    api.getUserCourses.and.returnValue(of([]));
    TestBed.inject(CourseSessionsResolver).resolve(route, state).subscribe((result) => {
      expect(result).toEqual([]);
      done();
    });
  });

  it('CourseSessionsResolver turns an error into null', (done) => {
    api.getUserCourses.and.returnValue(throwError(() => new Error('boom')));
    TestBed.inject(CourseSessionsResolver).resolve(route, state).subscribe((result) => {
      expect(result).toBeNull();
      done();
    });
  });

  it('UserCourseSessionsResolver passes a success through', (done) => {
    api.getUserCourseSessions.and.returnValue(of([]));
    TestBed.inject(UserCourseSessionsResolver).resolve(route, state).subscribe((result) => {
      expect(result).toEqual([]);
      done();
    });
  });

  it('UserCourseSessionsResolver turns an error into null', (done) => {
    api.getUserCourseSessions.and.returnValue(throwError(() => new Error('boom')));
    TestBed.inject(UserCourseSessionsResolver).resolve(route, state).subscribe((result) => {
      expect(result).toBeNull();
      done();
    });
  });
});

import { TestBed } from '@angular/core/testing';

import { AdminCourseResolver } from './admin-course.resolver';
import { APP_CONFIG } from "../../../app-config";
import { HttpClientTestingModule } from "@angular/common/http/testing";

describe('AdminCourseResolver', () => {
  let resolver: AdminCourseResolver;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [{
        provide: APP_CONFIG,
        useValue: {}
      }],
      imports: [
        HttpClientTestingModule,
      ]
    });
    resolver = TestBed.inject(AdminCourseResolver);
  });

  it('should be created', () => {
    expect(resolver).toBeTruthy();
  });
});

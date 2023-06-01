import { TestBed } from '@angular/core/testing';

import { UserCourseApiService } from './user-course-api.service';
import { APP_CONFIG } from "../../../app-config";
import { HttpClientTestingModule } from "@angular/common/http/testing";

describe('CourseApiService', () => {
  let service: UserCourseApiService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        {
          provide: APP_CONFIG,
          useValue: {}
        }
      ],
      imports: [
        HttpClientTestingModule,
      ]
    });
    service = TestBed.inject(UserCourseApiService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});

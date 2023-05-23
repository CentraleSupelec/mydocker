import { TestBed } from '@angular/core/testing';

import { CoursePermissionApiService } from './course-permission-api.service';
import { APP_CONFIG } from "../../../app-config";
import { HttpClientTestingModule } from "@angular/common/http/testing";

describe('CoursePermissionApiService', () => {
  let service: CoursePermissionApiService;

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
    service = TestBed.inject(CoursePermissionApiService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});

import { TestBed } from '@angular/core/testing';

import { AdminCoursesApiService } from './admin-courses-api.service';
import { APP_CONFIG } from "../../../app-config";
import { HttpClientTestingModule } from "@angular/common/http/testing";

describe('AdminCoursesApiService', () => {
  let service: AdminCoursesApiService;

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
    service = TestBed.inject(AdminCoursesApiService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});

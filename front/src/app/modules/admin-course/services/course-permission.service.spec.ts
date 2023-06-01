import { TestBed } from '@angular/core/testing';

import { CoursePermissionService } from './course-permission.service';
import { NgxPermissionsModule } from "ngx-permissions";

describe('CoursePermissionService', () => {
  let service: CoursePermissionService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [
        NgxPermissionsModule.forRoot(),
      ]
    });
    service = TestBed.inject(CoursePermissionService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});

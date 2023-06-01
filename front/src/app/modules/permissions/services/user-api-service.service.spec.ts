import { TestBed } from '@angular/core/testing';

import { UserApiServiceService } from './user-api-service.service';
import { APP_CONFIG } from "../../../app-config";
import { HttpClientTestingModule } from "@angular/common/http/testing";

describe('UserApiServiceService', () => {
  let service: UserApiServiceService;

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
    service = TestBed.inject(UserApiServiceService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});

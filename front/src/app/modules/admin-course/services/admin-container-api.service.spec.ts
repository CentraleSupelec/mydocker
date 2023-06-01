import { TestBed } from '@angular/core/testing';

import { AdminContainerApiService } from './admin-container-api.service';
import { APP_CONFIG } from "../../../app-config";
import { HttpClientTestingModule } from "@angular/common/http/testing";

describe('AdminContainerApiService', () => {
  let service: AdminContainerApiService;

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
    service = TestBed.inject(AdminContainerApiService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});

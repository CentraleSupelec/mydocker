import { TestBed } from '@angular/core/testing';

import { SessionWithResourcesApiService } from './session-with-resources-api.service';
import { APP_CONFIG } from "../../../app-config";
import { HttpClientTestingModule } from "@angular/common/http/testing";

describe('SessionWithResourcesApiService', () => {
  let service: SessionWithResourcesApiService;

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
    service = TestBed.inject(SessionWithResourcesApiService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});

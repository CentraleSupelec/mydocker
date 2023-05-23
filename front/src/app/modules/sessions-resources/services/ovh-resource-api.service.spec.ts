import { TestBed } from '@angular/core/testing';

import { OvhResourceApiService } from './ovh-resource-api.service';
import { HttpClientTestingModule } from "@angular/common/http/testing";
import { APP_CONFIG } from "../../../app-config";

describe('OvhResourceApiService', () => {
  let service: OvhResourceApiService;

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
    service = TestBed.inject(OvhResourceApiService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});

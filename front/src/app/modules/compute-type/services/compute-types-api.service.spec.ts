import { TestBed } from '@angular/core/testing';

import { ComputeTypesApiService } from './compute-types-api.service';
import { APP_CONFIG } from "../../../app-config";
import { HttpClientTestingModule } from "@angular/common/http/testing";

describe('ComputeTypesApiService', () => {
  let service: ComputeTypesApiService;

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
    service = TestBed.inject(ComputeTypesApiService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});

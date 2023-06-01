import { TestBed } from '@angular/core/testing';

import { RegionApiService } from './region-api.service';
import { HttpClientTestingModule } from "@angular/common/http/testing";
import { APP_CONFIG } from "../../../app-config";

describe('RegionApiService', () => {
  let service: RegionApiService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [
        HttpClientTestingModule,
      ],
      providers: [
        {
          provide: APP_CONFIG,
          useValue: {}
        }
      ]
    });
    service = TestBed.inject(RegionApiService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});

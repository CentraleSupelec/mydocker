import { TestBed } from '@angular/core/testing';

import { ContentsApiService } from './contents-api.service';
import { APP_CONFIG } from "../../../app-config";
import { HttpClientTestingModule } from "@angular/common/http/testing";

describe('ContentsApiService', () => {
  let service: ContentsApiService;

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
    service = TestBed.inject(ContentsApiService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});

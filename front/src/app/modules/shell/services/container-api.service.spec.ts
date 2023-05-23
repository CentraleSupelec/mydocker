import { TestBed } from '@angular/core/testing';

import { ContainerApiService } from './container-api.service';
import { APP_CONFIG } from "../../../app-config";
import { HttpClientTestingModule } from "@angular/common/http/testing";

describe('ContainerApiService', () => {
  let service: ContainerApiService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        {
          provide: APP_CONFIG,
          useValue: {}
        }
      ],
      imports: [
        HttpClientTestingModule,
      ]
    });
    service = TestBed.inject(ContainerApiService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});

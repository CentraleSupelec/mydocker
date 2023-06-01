import { TestBed } from '@angular/core/testing';

import { DeploymentApiService } from './deployment-api.service';
import { HttpClientTestingModule } from "@angular/common/http/testing";
import { APP_CONFIG } from "../../../app-config";

describe('DeploymentApiService', () => {
  let service: DeploymentApiService;

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
    service = TestBed.inject(DeploymentApiService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});

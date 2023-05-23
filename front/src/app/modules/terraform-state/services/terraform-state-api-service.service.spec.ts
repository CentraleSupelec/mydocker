import { TestBed } from '@angular/core/testing';

import { TerraformStateApiServiceService } from './terraform-state-api-service.service';
import { HttpClientTestingModule } from "@angular/common/http/testing";
import { APP_CONFIG } from "../../../app-config";

describe('TerraformStateApiServiceService', () => {
  let service: TerraformStateApiServiceService;

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
    service = TestBed.inject(TerraformStateApiServiceService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});

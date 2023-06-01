import { TestBed } from '@angular/core/testing';

import { DeploymentsResolver } from './deployments.resolver';
import { HttpClientTestingModule } from "@angular/common/http/testing";
import { APP_CONFIG } from "../../../app-config";

describe('DeploymentsResolver', () => {
  let resolver: DeploymentsResolver;

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
    resolver = TestBed.inject(DeploymentsResolver);
  });

  it('should be created', () => {
    expect(resolver).toBeTruthy();
  });
});

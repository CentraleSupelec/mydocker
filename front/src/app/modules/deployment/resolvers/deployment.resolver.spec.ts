import { TestBed } from '@angular/core/testing';

import { DeploymentResolver } from './deployment.resolver';
import { HttpClientTestingModule } from "@angular/common/http/testing";
import { APP_CONFIG } from "../../../app-config";

describe('DeploymentResolver', () => {
  let resolver: DeploymentResolver;

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
    resolver = TestBed.inject(DeploymentResolver);
  });

  it('should be created', () => {
    expect(resolver).toBeTruthy();
  });
});

import { TestBed } from '@angular/core/testing';

import { ComputeTypesResolver } from './compute-types.resolver';
import { APP_CONFIG } from '../../../app-config';
import { HttpClientTestingModule } from '@angular/common/http/testing';

describe('ComputeTypesResolverResolver', () => {
  let resolver: ComputeTypesResolver;

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
    resolver = TestBed.inject(ComputeTypesResolver);
  });

  it('should be created', () => {
    expect(resolver).toBeTruthy();
  });
});

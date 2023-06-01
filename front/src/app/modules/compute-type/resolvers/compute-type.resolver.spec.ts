import { TestBed } from '@angular/core/testing';

import { ComputeTypeResolver } from './compute-type.resolver';
import { APP_CONFIG } from '../../../app-config';
import { HttpClientTestingModule } from '@angular/common/http/testing';

describe('ComputeTypeResolver', () => {
  let resolver: ComputeTypeResolver;

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
    resolver = TestBed.inject(ComputeTypeResolver);
  });

  it('should be created', () => {
    expect(resolver).toBeTruthy();
  });
});

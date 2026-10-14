import { TestBed } from '@angular/core/testing';

import { ClustersResolver } from './clusters.resolver';
import { APP_CONFIG } from '../../../app-config';
import { HttpClientTestingModule } from '@angular/common/http/testing';

describe('ClustersResolver', () => {
  let resolver: ClustersResolver;

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
    resolver = TestBed.inject(ClustersResolver);
  });

  it('should be created', () => {
    expect(resolver).toBeTruthy();
  });
});

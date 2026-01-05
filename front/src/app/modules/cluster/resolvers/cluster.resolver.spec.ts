import { TestBed } from '@angular/core/testing';

import { ClusterResolver } from './cluster.resolver';
import { APP_CONFIG } from '../../../app-config';
import { HttpClientTestingModule } from '@angular/common/http/testing';

describe('ClusterResolver', () => {
  let resolver: ClusterResolver;

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
    resolver = TestBed.inject(ClusterResolver);
  });

  it('should be created', () => {
    expect(resolver).toBeTruthy();
  });
});

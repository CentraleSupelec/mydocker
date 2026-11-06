import { TestBed } from '@angular/core/testing';

import { SlurmClustersResolver } from './slurm-clusters.resolver';
import { APP_CONFIG } from '../../../app-config';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { NgxPermissionsModule } from 'ngx-permissions';

describe('SlurmClustersResolverResolver', () => {
  let resolver: SlurmClustersResolver;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        {
          provide: APP_CONFIG,
          useValue: {},
        },
      ],
      imports: [HttpClientTestingModule, NgxPermissionsModule.forRoot()],
    });
    resolver = TestBed.inject(SlurmClustersResolver);
  });

  it('should be created', () => {
    expect(resolver).toBeTruthy();
  });
});

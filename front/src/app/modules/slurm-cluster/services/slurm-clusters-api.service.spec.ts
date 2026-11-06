import { TestBed } from '@angular/core/testing';

import { SlurmClustersApiService } from './slurm-clusters-api.service';
import { APP_CONFIG } from '../../../app-config';
import { HttpClientTestingModule } from '@angular/common/http/testing';

describe('SlurmClustersApiService', () => {
  let service: SlurmClustersApiService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        {
          provide: APP_CONFIG,
          useValue: {},
        },
      ],
      imports: [HttpClientTestingModule],
    });
    service = TestBed.inject(SlurmClustersApiService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});

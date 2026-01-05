import { TestBed } from '@angular/core/testing';

import { APP_CONFIG } from '../../../app-config';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { ClustersBasicInformationResolver } from './clusters-basic-information.resolver';

describe('ClustersBasicInformationResolver', () => {
  let resolver: ClustersBasicInformationResolver;

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
    resolver = TestBed.inject(ClustersBasicInformationResolver);
  });

  it('should be created', () => {
    expect(resolver).toBeTruthy();
  });
});

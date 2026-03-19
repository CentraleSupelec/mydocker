import { TestBed } from '@angular/core/testing';

import { ContentsResolver } from './contents.resolver';
import { APP_CONFIG } from '../../../app-config';
import { HttpClientTestingModule } from '@angular/common/http/testing';

describe('ContentsResolverResolver', () => {
  let resolver: ContentsResolver;

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
    resolver = TestBed.inject(ContentsResolver);
  });

  it('should be created', () => {
    expect(resolver).toBeTruthy();
  });
});

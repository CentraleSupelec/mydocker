import { TestBed } from '@angular/core/testing';

import { ContentResolver } from './content.resolver';
import { APP_CONFIG } from '../../../app-config';
import { HttpClientTestingModule } from '@angular/common/http/testing';

describe('ContentResolver', () => {
  let resolver: ContentResolver;

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
    resolver = TestBed.inject(ContentResolver);
  });

  it('should be created', () => {
    expect(resolver).toBeTruthy();
  });
});

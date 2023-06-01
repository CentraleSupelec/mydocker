import { TestBed } from '@angular/core/testing';

import { RegionsResolver } from './regions.resolver';
import { HttpClientTestingModule } from "@angular/common/http/testing";
import { APP_CONFIG } from "../../../app-config";

describe('RegionsResolver', () => {
  let resolver: RegionsResolver;

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
    resolver = TestBed.inject(RegionsResolver);
  });

  it('should be created', () => {
    expect(resolver).toBeTruthy();
  });
});

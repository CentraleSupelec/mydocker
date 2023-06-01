import { TestBed } from '@angular/core/testing';

import { SessionWithResourceResolver } from './session-with-resource.resolver';
import { HttpClientTestingModule } from "@angular/common/http/testing";
import { APP_CONFIG } from "../../../app-config";

describe('SessionWithResourceResolver', () => {
  let resolver: SessionWithResourceResolver;

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
    resolver = TestBed.inject(SessionWithResourceResolver);
  });

  it('should be created', () => {
    expect(resolver).toBeTruthy();
  });
});

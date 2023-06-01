import { TestBed } from '@angular/core/testing';

import { OvhResourceResolver } from './ovh-resource.resolver';
import { HttpClientTestingModule } from "@angular/common/http/testing";
import { APP_CONFIG } from "../../../app-config";

describe('OvhResourceResolver', () => {
  let resolver: OvhResourceResolver;

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
    resolver = TestBed.inject(OvhResourceResolver);
  });

  it('should be created', () => {
    expect(resolver).toBeTruthy();
  });
});

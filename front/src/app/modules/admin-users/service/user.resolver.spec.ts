import { TestBed } from '@angular/core/testing';

import { UserResolver } from './user.resolver';
import { APP_CONFIG } from "../../../app-config";
import { HttpClientTestingModule } from "@angular/common/http/testing";

describe('UserResolver', () => {
  let resolver: UserResolver;

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
    resolver = TestBed.inject(UserResolver);
  });

  it('should be created', () => {
    expect(resolver).toBeTruthy();
  });
});

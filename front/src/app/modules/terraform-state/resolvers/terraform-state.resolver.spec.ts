import { TestBed } from '@angular/core/testing';

import { TerraformStateResolver } from './terraform-state.resolver';
import { HttpClientTestingModule } from "@angular/common/http/testing";
import { APP_CONFIG } from "../../../app-config";

describe('TerraformStateResolver', () => {
  let resolver: TerraformStateResolver;

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
    resolver = TestBed.inject(TerraformStateResolver);
  });

  it('should be created', () => {
    expect(resolver).toBeTruthy();
  });
});

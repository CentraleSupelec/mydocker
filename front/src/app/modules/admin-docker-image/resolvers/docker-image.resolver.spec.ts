import { TestBed } from '@angular/core/testing';

import { DockerImageResolver } from './docker-image.resolver';
import { AdminDockerImageModule } from "../admin-docker-image.module";
import { HttpClientTestingModule } from "@angular/common/http/testing";
import { APP_CONFIG } from "../../../app-config";

describe('DockerImageResolver', () => {
  let resolver: DockerImageResolver;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [
        HttpClientTestingModule,
      ],
      providers: [{
        provide: APP_CONFIG,
        useValue: {}
      }]
    });
    resolver = TestBed.inject(DockerImageResolver);
  });

  it('should be created', () => {
    expect(resolver).toBeTruthy();
  });
});

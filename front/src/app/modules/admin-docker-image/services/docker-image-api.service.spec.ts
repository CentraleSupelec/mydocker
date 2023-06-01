import { TestBed } from '@angular/core/testing';

import { DockerImageApiService } from './docker-image-api.service';
import { APP_CONFIG } from "../../../app-config";
import { HttpClientTestingModule } from "@angular/common/http/testing";

describe('DockerImageApiService', () => {
  let service: DockerImageApiService;

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
    service = TestBed.inject(DockerImageApiService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});

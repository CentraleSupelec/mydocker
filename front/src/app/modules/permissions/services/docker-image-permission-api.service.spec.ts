import { TestBed } from '@angular/core/testing';

import { DockerImagePermissionApiService } from './docker-image-permission-api.service';
import { APP_CONFIG } from "../../../app-config";
import { HttpClientTestingModule } from "@angular/common/http/testing";

describe('DockerImagePermissionApiService', () => {
  let service: DockerImagePermissionApiService;

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
    service = TestBed.inject(DockerImagePermissionApiService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});

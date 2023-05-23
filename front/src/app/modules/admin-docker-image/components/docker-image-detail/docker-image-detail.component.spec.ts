import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DockerImageDetailComponent } from './docker-image-detail.component';
import { AdminDockerImageModule } from "../../admin-docker-image.module";
import { RouterTestingModule } from "@angular/router/testing";
import { HttpClientTestingModule } from "@angular/common/http/testing";
import { APP_CONFIG } from "../../../../app-config";
import { NgxPermissionsModule } from "ngx-permissions";

describe('DockerImageDetailComponent', () => {
  let component: DockerImageDetailComponent;
  let fixture: ComponentFixture<DockerImageDetailComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ DockerImageDetailComponent ],
      imports: [
        AdminDockerImageModule,
        RouterTestingModule,
        HttpClientTestingModule,
        NgxPermissionsModule.forRoot(),
      ],
      providers: [
        {
          provide: APP_CONFIG,
          useValue: {}
        }
      ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(DockerImageDetailComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

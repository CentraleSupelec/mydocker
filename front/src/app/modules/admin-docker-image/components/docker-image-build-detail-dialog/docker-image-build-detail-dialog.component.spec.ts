import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DockerImageBuildDetailDialogComponent } from './docker-image-build-detail-dialog.component';
import { AdminDockerImageModule } from "../../admin-docker-image.module";
import { MAT_DIALOG_DATA, MatDialogRef } from "@angular/material/dialog";
import { APP_CONFIG } from "../../../../app-config";
import { HttpClientTestingModule } from "@angular/common/http/testing";

describe('DockerImageBuildDetailDialogComponent', () => {
  let component: DockerImageBuildDetailDialogComponent;
  let fixture: ComponentFixture<DockerImageBuildDetailDialogComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ DockerImageBuildDetailDialogComponent ],
      imports: [
        AdminDockerImageModule,
        HttpClientTestingModule,
      ],
      providers: [
        {
          provide: MAT_DIALOG_DATA,
          useValue: {}
        }, {
          provide: MatDialogRef,
          useValue: {}
        },
        {
          provide: APP_CONFIG,
          useValue: {}
        },
      ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(DockerImageBuildDetailDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

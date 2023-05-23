import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DockerImagePermissionDialogComponent } from './docker-image-permission-dialog.component';
import { PermissionsModule } from "../../permissions.module";
import { MAT_DIALOG_DATA, MatDialogRef } from "@angular/material/dialog";
import { HttpClientTestingModule } from "@angular/common/http/testing";
import { APP_CONFIG } from "../../../../app-config";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";

describe('DockerImagePermissionDialogComponent', () => {
  let component: DockerImagePermissionDialogComponent;
  let fixture: ComponentFixture<DockerImagePermissionDialogComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ DockerImagePermissionDialogComponent ],
      imports: [
        PermissionsModule,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [
        {
          provide: MAT_DIALOG_DATA,
          useValue: {}
        },
        {
          provide: MatDialogRef,
          useValue: {}
        },
        {
          provide: APP_CONFIG,
          useValue: {}
        }
      ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(DockerImagePermissionDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CoursePermissionDialogComponent } from './course-permission-dialog.component';
import { PermissionsModule } from "../../permissions.module";
import { MAT_DIALOG_DATA, MatDialogRef } from "@angular/material/dialog";
import { HttpClientTestingModule } from "@angular/common/http/testing";
import { APP_CONFIG } from "../../../../app-config";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";

describe('CoursePermissionDialogComponent', () => {
  let component: CoursePermissionDialogComponent;
  let fixture: ComponentFixture<CoursePermissionDialogComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ CoursePermissionDialogComponent ],
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
    fixture = TestBed.createComponent(CoursePermissionDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

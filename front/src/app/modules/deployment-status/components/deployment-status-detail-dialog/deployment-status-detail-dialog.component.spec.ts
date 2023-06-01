import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MAT_DIALOG_DATA, MatDialogRef } from "@angular/material/dialog";
import { DeploymentStatusModule } from "../../deployment-status.module";
import { DeploymentStatusDetailDialogComponent } from "./deployment-status-detail-dialog.component";
import { APP_CONFIG } from "../../../../app-config";
import { HttpClientTestingModule } from "@angular/common/http/testing";

describe('DeploymentStatusDetailDialogComponent', () => {
  let component: DeploymentStatusDetailDialogComponent;
  let fixture: ComponentFixture<DeploymentStatusDetailDialogComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ DeploymentStatusDetailDialogComponent ],
      imports: [
        DeploymentStatusModule,
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
        }
      ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(DeploymentStatusDetailDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

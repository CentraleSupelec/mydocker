import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DeploymentCreateComponent } from './deployment-create.component';
import { DeploymentModule } from "../../deployment.module";
import { HttpClientTestingModule } from "@angular/common/http/testing";
import { RouterTestingModule } from "@angular/router/testing";
import { APP_CONFIG } from "../../../../app-config";
import { SnackNotificationModule } from "../../../utils/snack-notification/snack-notification.module";
import { MatNativeDateModule } from "@angular/material/core";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateTestingModule } from 'src/testing/translate-testing.module';

describe('DeploymentCreateComponent', () => {
  let component: DeploymentCreateComponent;
  let fixture: ComponentFixture<DeploymentCreateComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ DeploymentCreateComponent ],
      imports: [
        TranslateTestingModule,
        DeploymentModule,
        HttpClientTestingModule,
        RouterTestingModule,
        SnackNotificationModule,
        MatNativeDateModule,
        NoopAnimationsModule,
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
    fixture = TestBed.createComponent(DeploymentCreateComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

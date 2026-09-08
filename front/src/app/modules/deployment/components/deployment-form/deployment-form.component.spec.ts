import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DeploymentFormComponent } from './deployment-form.component';
import { DeploymentModule } from "../../deployment.module";
import { HttpClientTestingModule } from "@angular/common/http/testing";
import { APP_CONFIG } from "../../../../app-config";
import { MatNativeDateModule } from "@angular/material/core";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateTestingModule } from 'src/testing/translate-testing.module';

describe('DeploymentFormComponent', () => {
  let component: DeploymentFormComponent;
  let fixture: ComponentFixture<DeploymentFormComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ DeploymentFormComponent ],
      imports: [
        TranslateTestingModule,
        DeploymentModule,
        HttpClientTestingModule,
        MatNativeDateModule,
        NoopAnimationsModule
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
    fixture = TestBed.createComponent(DeploymentFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

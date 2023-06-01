import { ComponentFixture, TestBed } from '@angular/core/testing';

import { LaunchWorkersFormComponent } from './launch-workers-form.component';
import { DeploymentModule } from "../../deployment.module";
import { HttpClientTestingModule } from "@angular/common/http/testing";
import { RouterTestingModule } from "@angular/router/testing";
import { APP_CONFIG } from "../../../../app-config";

describe('LaunchWorkersFormComponent', () => {
  let component: LaunchWorkersFormComponent;
  let fixture: ComponentFixture<LaunchWorkersFormComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ LaunchWorkersFormComponent ],
      imports: [
        DeploymentModule,
        HttpClientTestingModule,
        RouterTestingModule,
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
    fixture = TestBed.createComponent(LaunchWorkersFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

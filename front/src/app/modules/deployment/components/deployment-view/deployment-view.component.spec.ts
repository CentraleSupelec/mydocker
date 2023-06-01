import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DeploymentViewComponent } from './deployment-view.component';
import { DeploymentModule } from "../../deployment.module";
import { HttpClientTestingModule } from "@angular/common/http/testing";
import { RouterTestingModule } from "@angular/router/testing";

describe('DeploymentViewComponent', () => {
  let component: DeploymentViewComponent;
  let fixture: ComponentFixture<DeploymentViewComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ DeploymentViewComponent ],
      imports: [
        DeploymentModule,
        HttpClientTestingModule,
        RouterTestingModule,
      ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(DeploymentViewComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

import { ComponentFixture, TestBed } from '@angular/core/testing';

import { WorkerFormComponent } from './worker-form.component';
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { MatNativeDateModule } from "@angular/material/core";
import { DeploymentModule } from "../../deployment.module";

describe('SessionFormComponent', () => {
  let component: WorkerFormComponent;
  let fixture: ComponentFixture<WorkerFormComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ WorkerFormComponent ],
      imports: [
        DeploymentModule,
        NoopAnimationsModule,
        MatNativeDateModule,
      ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(WorkerFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

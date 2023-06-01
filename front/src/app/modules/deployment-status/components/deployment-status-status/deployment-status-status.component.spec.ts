import { ComponentFixture, TestBed } from '@angular/core/testing';
import { DeploymentStatusStatusComponent } from "./deployment-status-status.component";

describe('DockerImageBuildStatusComponent', () => {
  let component: DeploymentStatusStatusComponent;
  let fixture: ComponentFixture<DeploymentStatusStatusComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ DeploymentStatusStatusComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(DeploymentStatusStatusComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

import { ComponentFixture, TestBed } from '@angular/core/testing';

import { RouterTestingModule } from "@angular/router/testing";
import { HttpClientTestingModule } from "@angular/common/http/testing";
import { DeploymentStatusDetailComponent } from "./deployment-status-detail.component";
import { DeploymentStatusModule } from "../../deployment-status.module";
import { APP_CONFIG } from "../../../../app-config";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateTestingModule } from 'src/testing/translate-testing.module';
import { ActivatedRoute } from '@angular/router';
import { of } from 'rxjs';

describe('DeploymentStatusDetailComponent', () => {
  let component: DeploymentStatusDetailComponent;
  let fixture: ComponentFixture<DeploymentStatusDetailComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ DeploymentStatusDetailComponent ],
      imports: [
        TranslateTestingModule,
        DeploymentStatusModule,
        RouterTestingModule,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [
        {
          provide: ActivatedRoute,
          useValue: {
            data: of({ clusters: [{ id: 42, name: 'Production' }] })
          }
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
    fixture = TestBed.createComponent(DeploymentStatusDetailComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('shows the cluster name in the deployment status list', () => {
    component.deploymentsStatus = [{
      id: 1,
      cluster: 42,
      logs: '',
      buildErrors: '',
      status: 'DONE',
      createdOn: 0,
      updatedOn: 0
    }];
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('mat-cell.mat-column-cluster').textContent.trim())
      .toBe('Production');
  });

  it('shows a placeholder for a deployment without a cluster', () => {
    component.deploymentsStatus = [{
      id: 1,
      cluster: null,
      logs: '',
      buildErrors: '',
      status: 'DONE',
      createdOn: 0,
      updatedOn: 0
    }];
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('mat-cell.mat-column-cluster').textContent.trim())
      .toBe('-');
  });

  it('shows the cluster id when its name is unavailable', () => {
    component.deploymentsStatus = [{
      id: 1,
      cluster: 99,
      logs: '',
      buildErrors: '',
      status: 'DONE',
      createdOn: 0,
      updatedOn: 0
    }];
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('mat-cell.mat-column-cluster').textContent.trim())
      .toBe('99');
  });
});

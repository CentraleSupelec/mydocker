import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DeploymentFormComponent } from './deployment-form.component';
import { DeploymentModule } from "../../deployment.module";
import { HttpClientTestingModule } from "@angular/common/http/testing";
import { APP_CONFIG } from "../../../../app-config";
import { MatNativeDateModule } from "@angular/material/core";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateTestingModule } from 'src/testing/translate-testing.module';
import { IDeploymentSession } from "../../interfaces/deployment-session";
import { DeploymentApiService } from "../../services/deployment-api.service";
import { Subject } from "rxjs";

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

  describe('sessions and cluster', () => {
    const session = (id: number, cluster: number | null) => ({ id, cluster } as IDeploymentSession);

    beforeEach(() => {
      component.sessions = [session(1, 1), session(2, 2), session(3, null)];
      component.deploymentForm.get('sessions')?.setValue([1, 2, 3]);
    });

    it('lists every session before a cluster is chosen', () => {
      expect(component.sessionsForCluster.map(s => s.id)).toEqual([1, 2, 3]);
    });

    it('lists only the chosen cluster and unassigned courses', () => {
      component.deploymentForm.get('cluster')?.setValue(1);

      expect(component.sessionsForCluster.map(s => s.id)).toEqual([1, 3]);
    });

    it('drops selected sessions of another cluster', () => {
      component.deploymentForm.get('cluster')?.setValue(1);

      expect(component.deploymentForm.get('sessions')?.value).toEqual([1, 3]);
    });

    it('drops a hidden incompatible session once the list arrives after the cluster', () => {
      const api = TestBed.inject(DeploymentApiService);
      const list = new Subject<IDeploymentSession[]>();
      spyOn(api, 'getSessionToLaunch').and.returnValue(list);
      component.deploymentForm.get('type')?.setValue('launch');
      component.sessions = [];
      component.deploymentForm.get('sessions')?.setValue([1, 2, 3]);
      component.deploymentForm.get('cluster')?.setValue(1);
      expect(component.deploymentForm.get('sessions')?.value).toEqual([1, 2, 3]);

      list.next([session(1, 1), session(2, 2), session(3, null)]);

      expect(component.deploymentForm.get('sessions')?.value).toEqual([1, 3]);
      expect(component.selectedSessions.map(s => s.id)).toEqual([1, 3]);
    });

    it('keeps selected sessions not loaded yet, as on edit', () => {
      component.sessions = [];
      component.deploymentForm.get('cluster')?.setValue(1);

      expect(component.deploymentForm.get('sessions')?.value).toEqual([1, 2, 3]);
    });
  });
});

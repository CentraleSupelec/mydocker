import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DeploymentListComponent } from './deployment-list.component';
import { DeploymentModule } from "../../deployment.module";
import { HttpClientTestingModule } from "@angular/common/http/testing";
import { RouterTestingModule } from "@angular/router/testing";
import { APP_CONFIG } from "../../../../app-config";
import { ActivatedRoute } from "@angular/router";
import { of } from "rxjs";

describe('DeploymentListComponent', () => {
  let component: DeploymentListComponent;
  let fixture: ComponentFixture<DeploymentListComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ DeploymentListComponent ],
      imports: [
        DeploymentModule,
        HttpClientTestingModule,
        RouterTestingModule,
      ],
      providers: [
        {
          provide: APP_CONFIG,
          useValue: {}
        },
        {
          provide: ActivatedRoute,
          useValue: {
            data: of({deployments: []}),
            snapshot: {}
          }
        }
      ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(DeploymentListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

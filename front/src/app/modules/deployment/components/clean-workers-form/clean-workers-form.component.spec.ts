import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CleanWorkersFormComponent } from './clean-workers-form.component';
import { DeploymentModule } from "../../deployment.module";
import { HttpClientTestingModule } from "@angular/common/http/testing";
import { RouterTestingModule } from "@angular/router/testing";
import { APP_CONFIG } from "../../../../app-config";
import { TranslateTestingModule } from 'src/testing/translate-testing.module';

describe('CleanWorkersFormComponent', () => {
  let component: CleanWorkersFormComponent;
  let fixture: ComponentFixture<CleanWorkersFormComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ CleanWorkersFormComponent ],
      imports: [
        TranslateTestingModule,
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
    fixture = TestBed.createComponent(CleanWorkersFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

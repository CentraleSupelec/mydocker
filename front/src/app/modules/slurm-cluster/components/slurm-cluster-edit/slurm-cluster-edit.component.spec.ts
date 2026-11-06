import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SlurmClusterEditComponent } from './slurm-cluster-edit.component';
import { SlurmClusterModule } from '../../slurm-cluster.module';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { APP_CONFIG } from '../../../../app-config';
import { TranslateModule } from '@ngx-translate/core';

describe('SlurmClusterEditComponent', () => {
  let component: SlurmClusterEditComponent;
  let fixture: ComponentFixture<SlurmClusterEditComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      providers: [
        {
          provide: APP_CONFIG,
          useValue: {
            default_storage_backend: 'RBD',
          },
        },
      ],
      declarations: [SlurmClusterEditComponent],
      imports: [
        SlurmClusterModule,
        HttpClientTestingModule,
        RouterTestingModule,
        NoopAnimationsModule,
        TranslateModule.forRoot(),
      ],
    }).compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(SlurmClusterEditComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

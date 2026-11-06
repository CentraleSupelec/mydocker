import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SlurmClusterNewComponent } from './slurm-cluster-new.component';
import { SlurmClusterModule } from '../../slurm-cluster.module';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { APP_CONFIG } from '../../../../app-config';
import { RouterTestingModule } from '@angular/router/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { TranslateModule } from '@ngx-translate/core';

describe('SlurmClusterNewComponent', () => {
  let component: SlurmClusterNewComponent;
  let fixture: ComponentFixture<SlurmClusterNewComponent>;

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
      declarations: [SlurmClusterNewComponent],
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
    fixture = TestBed.createComponent(SlurmClusterNewComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

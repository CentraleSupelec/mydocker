import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SlurmClusterListComponent } from './slurm-cluster-list.component';
import { SlurmClusterModule } from '../../slurm-cluster.module';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { APP_CONFIG } from '../../../../app-config';
import { TranslateModule } from '@ngx-translate/core';

describe('SlurmClusterListComponent', () => {
  let component: SlurmClusterListComponent;
  let fixture: ComponentFixture<SlurmClusterListComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      providers: [
        {
          provide: APP_CONFIG,
          useValue: {},
        },
      ],
      declarations: [SlurmClusterListComponent],
      imports: [
        SlurmClusterModule,
        HttpClientTestingModule,
        RouterTestingModule,
        TranslateModule.forRoot(),
      ],
    }).compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(SlurmClusterListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SlurmClusterFormComponent } from './slurm-cluster-form.component';
import { SlurmClusterModule } from '../../slurm-cluster.module';
import { TranslateModule } from '@ngx-translate/core';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

describe('SlurmClusterFormComponent', () => {
  let component: SlurmClusterFormComponent;
  let fixture: ComponentFixture<SlurmClusterFormComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [SlurmClusterFormComponent],
      imports: [
        SlurmClusterModule,
        TranslateModule.forRoot(),
        NoopAnimationsModule,
      ],
    }).compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(SlurmClusterFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

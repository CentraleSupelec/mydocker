import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ClusterFormComponent } from './cluster-form.component';
import { ClusterModule } from '../../cluster.module';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { TranslateTestingModule } from 'src/testing/translate-testing.module';

describe('ClusterFormComponent', () => {
  let component: ClusterFormComponent;
  let fixture: ComponentFixture<ClusterFormComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ ClusterFormComponent ],
      imports: [
        ClusterModule,
        NoopAnimationsModule,
        TranslateTestingModule
      ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ClusterFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

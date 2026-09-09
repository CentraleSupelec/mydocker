import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ResourcesRegionsFormComponent } from './resources-regions-form.component';
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule } from "@ngx-translate/core";
import { ReactiveFormsModule } from "@angular/forms";

describe('ResourcesRegionsFormComponent', () => {
  let component: ResourcesRegionsFormComponent;
  let fixture: ComponentFixture<ResourcesRegionsFormComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ ResourcesRegionsFormComponent ],
      imports: [
        NoopAnimationsModule,
        TranslateModule.forRoot(),
        ReactiveFormsModule,
      ],
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ResourcesRegionsFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

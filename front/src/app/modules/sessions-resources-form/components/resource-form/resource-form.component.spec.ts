import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ResourceFormComponent } from './resource-form.component';
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { MatNativeDateModule } from "@angular/material/core";
import { SessionsResourcesFormModule } from "../../sessions-form.module";
import { TranslateTestingModule } from 'src/testing/translate-testing.module';

describe('SessionFormComponent', () => {
  let component: ResourceFormComponent;
  let fixture: ComponentFixture<ResourceFormComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ ResourceFormComponent ],
      imports: [
        TranslateTestingModule,
        SessionsResourcesFormModule,
        NoopAnimationsModule,
        MatNativeDateModule,
      ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ResourceFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

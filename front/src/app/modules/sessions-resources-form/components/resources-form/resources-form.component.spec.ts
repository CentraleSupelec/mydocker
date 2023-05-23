import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ResourcesFormComponent } from './resources-form.component';
import { SessionsResourcesFormModule } from "../../sessions-form.module";

describe('SessionsFormComponent', () => {
  let component: ResourcesFormComponent;
  let fixture: ComponentFixture<ResourcesFormComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ ResourcesFormComponent ],
      imports: [
        SessionsResourcesFormModule,
      ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ResourcesFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

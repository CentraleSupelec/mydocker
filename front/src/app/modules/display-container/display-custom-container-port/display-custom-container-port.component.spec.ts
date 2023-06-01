import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DisplayCustomContainerPortComponent } from './display-custom-container-port.component';

describe('DisplayCustomContainerPortComponent', () => {
  let component: DisplayCustomContainerPortComponent;
  let fixture: ComponentFixture<DisplayCustomContainerPortComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ DisplayCustomContainerPortComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(DisplayCustomContainerPortComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

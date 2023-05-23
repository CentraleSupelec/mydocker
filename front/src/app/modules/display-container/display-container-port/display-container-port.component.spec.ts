import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DisplayContainerPortComponent } from './display-container-port.component';

describe('DisplayContainerPortComponent', () => {
  let component: DisplayContainerPortComponent;
  let fixture: ComponentFixture<DisplayContainerPortComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ DisplayContainerPortComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(DisplayContainerPortComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

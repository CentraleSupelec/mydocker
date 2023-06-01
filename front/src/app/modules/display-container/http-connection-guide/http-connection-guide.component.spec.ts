import { ComponentFixture, TestBed } from '@angular/core/testing';

import { HttpConnectionGuideComponent } from './http-connection-guide.component';

describe('HttpConnexionGuideComponent', () => {
  let component: HttpConnectionGuideComponent;
  let fixture: ComponentFixture<HttpConnectionGuideComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ HttpConnectionGuideComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(HttpConnectionGuideComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

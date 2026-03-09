import { ComponentFixture, TestBed } from '@angular/core/testing';

import { TcpConnectionGuideComponent } from './tcp-connection-guide.component';
import { RouterTestingModule } from "@angular/router/testing";
import { DisplayContainerModule } from "../display-container.module";

describe('TcpConnexionGuideComponent', () => {
  let component: TcpConnectionGuideComponent;
  let fixture: ComponentFixture<TcpConnectionGuideComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ TcpConnectionGuideComponent ],
      imports: [
        DisplayContainerModule,
        RouterTestingModule,
      ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(TcpConnectionGuideComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

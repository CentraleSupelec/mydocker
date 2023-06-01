import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SshConnectionGuideComponent } from './ssh-connection-guide.component';
import { MatIconModule } from "@angular/material/icon";
import { MatButtonModule } from "@angular/material/button";
import { RouterTestingModule } from "@angular/router/testing";
import { DisplayContainerModule } from "../display-container.module";

describe('SshConnexionGuideComponent', () => {
  let component: SshConnectionGuideComponent;
  let fixture: ComponentFixture<SshConnectionGuideComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ SshConnectionGuideComponent ],
      imports: [
        DisplayContainerModule,
        RouterTestingModule,
      ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(SshConnectionGuideComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

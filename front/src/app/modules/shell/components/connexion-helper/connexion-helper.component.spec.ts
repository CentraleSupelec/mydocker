import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ConnexionHelperComponent } from './connexion-helper.component';

describe('ConnexionHelperComponent', () => {
  let component: ConnexionHelperComponent;
  let fixture: ComponentFixture<ConnexionHelperComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ ConnexionHelperComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ConnexionHelperComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

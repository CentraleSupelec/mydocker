import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ConnexionHelperComponent } from './connexion-helper.component';
import { TranslateTestingModule } from 'src/testing/translate-testing.module';

describe('ConnexionHelperComponent', () => {
  let component: ConnexionHelperComponent;
  let fixture: ComponentFixture<ConnexionHelperComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TranslateTestingModule],
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

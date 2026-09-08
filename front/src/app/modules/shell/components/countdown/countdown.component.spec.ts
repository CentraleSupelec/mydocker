import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CountdownComponent } from './countdown.component';
import { APP_CONFIG } from '../../../../app-config';
import { ShellModule } from '../../shell.module';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { TranslateTestingModule } from 'src/testing/translate-testing.module';

describe('CountdownComponent', () => {
  let component: CountdownComponent;
  let fixture: ComponentFixture<CountdownComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ CountdownComponent ],
      providers: [
        {
          provide: APP_CONFIG,
          useValue: {}
        },
      ],
      imports: [
        TranslateTestingModule,
        ShellModule,
        HttpClientTestingModule,
        RouterTestingModule,
      ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(CountdownComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

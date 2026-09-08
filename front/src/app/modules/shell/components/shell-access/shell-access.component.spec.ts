import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ShellAccessComponent } from './shell-access.component';
import { APP_CONFIG } from "../../../../app-config";
import { ShellModule } from "../../shell.module";
import { HttpClientTestingModule } from "@angular/common/http/testing";
import { RouterTestingModule } from "@angular/router/testing";
import { NgxPermissionsModule } from "ngx-permissions";
import { TranslateTestingModule } from 'src/testing/translate-testing.module';

describe('ShellAccessComponent', () => {
  let component: ShellAccessComponent;
  let fixture: ComponentFixture<ShellAccessComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ ShellAccessComponent ],
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
        NgxPermissionsModule.forRoot(),
      ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ShellAccessComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

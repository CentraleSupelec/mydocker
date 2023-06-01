import { ComponentFixture, TestBed } from '@angular/core/testing';

import { BasicLayoutComponent } from './basic-layout.component';
import { APP_CONFIG } from "../../../../app-config";
import { UserInterfaceModule } from "../../user-interface.module";
import { NgxPermissionsModule } from "ngx-permissions";
import { HttpClientTestingModule } from "@angular/common/http/testing";
import { RouterTestingModule } from "@angular/router/testing";

describe('BasicLayoutComponent', () => {
  let component: BasicLayoutComponent;
  let fixture: ComponentFixture<BasicLayoutComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ BasicLayoutComponent ],
      imports: [
        UserInterfaceModule,
        NgxPermissionsModule.forRoot(),
        HttpClientTestingModule,
        RouterTestingModule,
      ],
      providers: [
        {
          provide: APP_CONFIG,
          useValue: {}
        }
      ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(BasicLayoutComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

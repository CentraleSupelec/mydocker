import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MAT_DIALOG_DATA, MatDialogRef } from "@angular/material/dialog";
import { LogDialogComponent } from "./log-dialog.component";
import { LogDialogModule } from "../log-dialog.module";

describe('LogDialogComponent', () => {
  let component: LogDialogComponent;
  let fixture: ComponentFixture<LogDialogComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ LogDialogComponent ],
      imports: [
        LogDialogModule,
      ],
      providers: [
        {
          provide: MAT_DIALOG_DATA,
          useValue: {}
        }, {
          provide: MatDialogRef,
          useValue: {}
        },
      ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(LogDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

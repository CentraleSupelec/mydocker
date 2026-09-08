import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SaveStateComponent } from './save-state.component';
import { ShellModule } from "../../shell.module";
import { HttpClientTestingModule } from "@angular/common/http/testing";
import { RouterTestingModule } from "@angular/router/testing";
import { TranslateTestingModule } from 'src/testing/translate-testing.module';

describe('SaveStateComponent', () => {
  let component: SaveStateComponent;
  let fixture: ComponentFixture<SaveStateComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ SaveStateComponent ],
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
    fixture = TestBed.createComponent(SaveStateComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

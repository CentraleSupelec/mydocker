import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DisplayTerraformStateComponent } from './display-terraform-state.component';
import { TerraformStateModule } from "../../terraform-state.module";
import { HttpClientTestingModule } from "@angular/common/http/testing";
import { RouterTestingModule } from "@angular/router/testing";

describe('DisplayTerraformStateComponent', () => {
  let component: DisplayTerraformStateComponent;
  let fixture: ComponentFixture<DisplayTerraformStateComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ DisplayTerraformStateComponent ],
      imports: [
        TerraformStateModule,
        HttpClientTestingModule,
        RouterTestingModule,
      ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(DisplayTerraformStateComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

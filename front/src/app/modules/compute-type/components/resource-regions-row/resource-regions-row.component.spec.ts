import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ResourceRegionsRowComponent } from './resource-regions-row.component';
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { MatNativeDateModule } from "@angular/material/core";
import { TranslateModule } from "@ngx-translate/core";
import { ReactiveFormsModule } from "@angular/forms";
import { ComputeTypeModule } from "../../compute-type.module";
import { HttpClientTestingModule } from "@angular/common/http/testing";

describe('ResourceRegionsRowComponent', () => {
  let component: ResourceRegionsRowComponent;
  let fixture: ComponentFixture<ResourceRegionsRowComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ ResourceRegionsRowComponent ],
      imports: [
        ComputeTypeModule,
        NoopAnimationsModule,
        MatNativeDateModule,
        TranslateModule.forRoot(),
        ReactiveFormsModule,
        HttpClientTestingModule,
      ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ResourceRegionsRowComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

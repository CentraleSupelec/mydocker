import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DockerImageEditComponent } from './docker-image-edit.component';
import { AdminDockerImageModule } from "../../admin-docker-image.module";
import { RouterTestingModule } from "@angular/router/testing";
import { HttpClientTestingModule } from "@angular/common/http/testing";
import { MonacoEditorModule } from "ngx-monaco-editor";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { NgxPermissionsModule } from "ngx-permissions";

describe('DockerImageEditComponent', () => {
  let component: DockerImageEditComponent;
  let fixture: ComponentFixture<DockerImageEditComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ DockerImageEditComponent ],
      imports: [
        AdminDockerImageModule,
        RouterTestingModule,
        HttpClientTestingModule,
        MonacoEditorModule.forRoot(),
        NoopAnimationsModule,
        NgxPermissionsModule.forRoot(),
      ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(DockerImageEditComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

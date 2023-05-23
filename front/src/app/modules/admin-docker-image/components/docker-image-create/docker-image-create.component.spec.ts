import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DockerImageCreateComponent } from './docker-image-create.component';
import { AdminDockerImageModule } from "../../admin-docker-image.module";
import { HttpClientTestingModule } from "@angular/common/http/testing";
import { RouterTestingModule } from "@angular/router/testing";
import { MonacoEditorModule } from "ngx-monaco-editor";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { NgxPermissionsModule } from "ngx-permissions";

describe('DockerImageCreateComponent', () => {
  let component: DockerImageCreateComponent;
  let fixture: ComponentFixture<DockerImageCreateComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ DockerImageCreateComponent ],
      imports: [
        AdminDockerImageModule,
        HttpClientTestingModule,
        RouterTestingModule,
        MonacoEditorModule.forRoot(),
        NoopAnimationsModule,
        NgxPermissionsModule.forRoot(),
      ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(DockerImageCreateComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DockerImageFormComponent } from './docker-image-form.component';
import { AdminDockerImageModule } from "../../admin-docker-image.module";
import { MonacoEditorModule } from "ngx-monaco-editor";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";

describe('DockerImageFormComponent', () => {
  let component: DockerImageFormComponent;
  let fixture: ComponentFixture<DockerImageFormComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ DockerImageFormComponent ],
      imports: [
        AdminDockerImageModule,
        MonacoEditorModule.forRoot(),
        NoopAnimationsModule,
      ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(DockerImageFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

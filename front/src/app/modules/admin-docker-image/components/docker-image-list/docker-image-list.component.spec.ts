import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DockerImageListComponent } from './docker-image-list.component';
import { AdminDockerImageModule } from "../../admin-docker-image.module";
import { HttpClientTestingModule } from "@angular/common/http/testing";
import { RouterTestingModule } from "@angular/router/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";

describe('DockerImageListComponent', () => {
  let component: DockerImageListComponent;
  let fixture: ComponentFixture<DockerImageListComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ DockerImageListComponent ],
      imports: [
        AdminDockerImageModule,
        HttpClientTestingModule,
        RouterTestingModule,
        NoopAnimationsModule
      ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(DockerImageListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

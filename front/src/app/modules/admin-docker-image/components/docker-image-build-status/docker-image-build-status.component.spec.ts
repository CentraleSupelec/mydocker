import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DockerImageBuildStatusComponent } from './docker-image-build-status.component';

describe('DockerImageBuildStatusComponent', () => {
  let component: DockerImageBuildStatusComponent;
  let fixture: ComponentFixture<DockerImageBuildStatusComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ DockerImageBuildStatusComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(DockerImageBuildStatusComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { ReactiveFormsModule } from "@angular/forms";
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from "@angular/material/dialog";
import { ActivatedRoute } from "@angular/router";
import { MatAutocompleteModule, MatAutocompleteTrigger } from "@angular/material/autocomplete";
import { MatInputModule } from "@angular/material/input";
import { MatSelectModule } from "@angular/material/select";
import { By } from "@angular/platform-browser";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule, TranslateService } from "@ngx-translate/core";
import { of } from "rxjs";

import { DockerImageChoiceDialogComponent } from './docker-image-choice-dialog.component';
import { DockerImageApiService } from "../../../admin-docker-image/services/docker-image-api.service";
import { IDockerImage } from "../../../admin-docker-image/interfaces/docker-image";
import { UtilsModule } from "../../../utils/utils.module";

describe('DockerImageChoiceDialogComponent', () => {
  let component: DockerImageChoiceDialogComponent;
  let fixture: ComponentFixture<DockerImageChoiceDialogComponent>;
  let dockerImageApiService: jasmine.SpyObj<DockerImageApiService>;

  const image = (lastBuildOn: number | null): IDockerImage => ({
    id: 1,
    name: 'my-image',
    description: '',
    dockerFile: '',
    wrapperScript: '',
    contextFolderName: '',
    ports: [],
    lastStatus: '',
    creator: '',
    visible: false,
    createdOn: 0,
    lastBuildOn,
  });

  const builtImage = image(new Date(2026, 11, 29, 12, 0).getTime());

  beforeEach(async () => {
    dockerImageApiService = jasmine.createSpyObj<DockerImageApiService>(
      'DockerImageApiService', ['getDockerImages', 'getDockerImageBuild']
    );
    dockerImageApiService.getDockerImages.and.returnValue(of({ content: [], totalElements: 0 }));

    await TestBed.configureTestingModule({
      declarations: [ DockerImageChoiceDialogComponent ],
      imports: [
        TranslateModule.forRoot(),
        ReactiveFormsModule,
        MatDialogModule,
        MatAutocompleteModule,
        MatInputModule,
        MatSelectModule,
        UtilsModule,
        NoopAnimationsModule,
      ],
      providers: [
        { provide: DockerImageApiService, useValue: dockerImageApiService },
        { provide: MatDialogRef, useValue: jasmine.createSpyObj('MatDialogRef', ['close']) },
        { provide: MAT_DIALOG_DATA, useValue: { clusters: [] } },
        { provide: ActivatedRoute, useValue: {} },
      ],
      // app-docker-image-build-status only appears in the build selector, out of scope here
      schemas: [ CUSTOM_ELEMENTS_SCHEMA ]
    })
    .compileComponents();
  });

  function create(images: IDockerImage[] = []) {
    dockerImageApiService.getDockerImages.and.returnValue(of({ content: images, totalElements: images.length }));
    fixture = TestBed.createComponent(DockerImageChoiceDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  function imageInput(): HTMLInputElement {
    return fixture.debugElement.query(By.css('input[aria-label="Image docker"]')).nativeElement;
  }

  function optionTexts(): string[] {
    fixture.debugElement.query(By.directive(MatAutocompleteTrigger))
      .injector.get(MatAutocompleteTrigger).openPanel();
    fixture.detectChanges();
    return Array.from(document.querySelectorAll('mat-option'))
      .map(option => (option.textContent ?? '').trim());
  }

  it('asks for 50 images sorted by last build on open', () => {
    create();
    expect(dockerImageApiService.getDockerImages).toHaveBeenCalledWith('', 0, 50, 'lastBuildOn', 'desc');
  });

  it('keeps the size and sort when searching', () => {
    create();
    const input = imageInput();
    input.value = 'abc';
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    expect(dockerImageApiService.getDockerImages).toHaveBeenCalledWith('abc', 0, 50, 'lastBuildOn', 'desc');
  });

  it('shows the last build date after the name', () => {
    create([builtImage]);
    expect(optionTexts()).toEqual(['my-image - 29/12/2026']);
  });

  it('shows the name alone when never built', () => {
    create([image(null)]);
    expect(optionTexts()).toEqual(['my-image']);
  });

  it('fills the input with the name alone', () => {
    create();
    expect(component.displayFn(builtImage)).toBe('my-image');
  });

  it('shows a search placeholder in the image field', () => {
    const translate = TestBed.inject(TranslateService);
    translate.setTranslation('en', {
      admin: { courses: { edit: { technical_info: { docker_image: { placeholder: 'Type to search an image' } } } } }
    });
    translate.use('en');
    create();
    expect(imageInput().getAttribute('placeholder')).toBe('Type to search an image');
  });
});

import {
  Component,
  forwardRef,
  HostListener,
  Input,
  OnDestroy,
  OnInit, Output,
  ViewChild,
  ViewContainerRef
} from '@angular/core';
import {
  AbstractControl,
  ControlValueAccessor,
  FormBuilder,
  FormGroup,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validator,
  Validators
} from "@angular/forms";
import { Subject } from "rxjs";
import { ICreationDockerImage, IDockerImage } from "../../interfaces/docker-image";
import { takeUntil } from "rxjs/operators";

@Component({
  selector: 'app-docker-image-form',
  templateUrl: './docker-image-form.component.html',
  styleUrls: ['./docker-image-form.component.css'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => DockerImageFormComponent),
      multi: true,
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => DockerImageFormComponent),
      multi: true,
    }
  ]
})
export class DockerImageFormComponent implements OnInit, ControlValueAccessor, Validator, OnDestroy {
  form: FormGroup;
  @Input() editMode = false;
  @Output() download = new Subject<void>();

  canDownloadFile = false;

  editorOptionsDockerFile = {
    language: 'dockerfile',
    readOnly: false
  };
  editorOptionsWrapperScript = {
    language: 'shell',
    readOnly: false
  };
  editorDockerFile: any;
  editorWrapperScript: any;
  @ViewChild('matTabGroup', {read: ViewContainerRef, static: true}) matTabGroupRef: ViewContainerRef | undefined;

  private destroy$ = new Subject<void>();
  private propagateChange = (_: ICreationDockerImage) => {}

  constructor(
    formBuilder: FormBuilder,
  ) {
    this.form = formBuilder.group({
      name: ['', [Validators.required, Validators.pattern('^[a-z0-9-_]+$')]],
      description: ['', Validators.required],
      dockerFile: ['', Validators.required],
      wrapperScript: ['', Validators.required],
      visible: [false, Validators.required],
      ports: [[]],
      contextFolder: null,
    })
  }

  ngOnInit(): void {
    this.form.valueChanges
      .pipe(
        takeUntil(this.destroy$)
      )
      .subscribe(
        (value) => {
          this.propagateChange(value)
        }
      )
  }

  ngOnDestroy(): void {
    this.destroy$.next();
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {}

  validate(control: AbstractControl): ValidationErrors | null {
    if (this.form.invalid) {
      return {dockerImage: this.form.errors}
    }
    return null;
  }

  writeValue(obj: IDockerImage): void {
    this.form.setValue({
      name: obj?.name || '',
      description: obj?.description || '',
      dockerFile: obj?.dockerFile || '',
      wrapperScript: obj?.wrapperScript || '',
      ports: obj?.ports || [],
      contextFolder: obj?.contextFolderName ? new File([], obj.contextFolderName): null,
      visible: obj?.visible || false,
    })
    this.canDownloadFile = !!obj?.contextFolderName && !!this.download
  }

  @HostListener('window:resize')
  updateLayout() {
    if (this.editorDockerFile) {
      this.editorDockerFile.layout();
    }
    if (this.editorWrapperScript) {
      this.editorWrapperScript.layout();
    }
  }

  onInitDockerFileEditor(editorDockerFile: any) {
    this.editorDockerFile = editorDockerFile;
    this.updateDockerFileEditor()
  }

  onInitWrapperScriptEditor(editorWrapperScript: any) {
    this.editorWrapperScript = editorWrapperScript;
    this.updateWrapperScriptEditor()
  }

  setDisabledState(isDisabled: boolean) {
    if (isDisabled) {
      this.form.disable();
    } else {
      this.form.enable();
    }
    this.editorOptionsDockerFile.readOnly = isDisabled
    this.editorOptionsWrapperScript.readOnly = isDisabled
    this.updateDockerFileEditor()
    this.updateWrapperScriptEditor()
  }

  private updateDockerFileEditor() {
    if (this.editorDockerFile) {
      this.editorDockerFile.updateOptions({
        readOnly: this.editorOptionsDockerFile.readOnly
      })
    }
  }

  private updateWrapperScriptEditor() {
    if (this.editorWrapperScript) {
      this.editorWrapperScript.updateOptions({
        readOnly: this.editorOptionsWrapperScript.readOnly
      })
    }
  }

  updateFile($event: FileList | File) {
    this.form.get('contextFolder')?.setValue($event);
  }
}

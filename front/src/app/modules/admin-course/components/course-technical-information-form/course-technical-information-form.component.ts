import { Component, forwardRef, Inject, Input, OnDestroy, OnInit } from '@angular/core';
import { APP_CONFIG, IAppConfig } from "../../../../app-config";
import {
  AbstractControl,
  ControlValueAccessor,
  FormBuilder,
  FormGroup,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validator,
  ValidatorFn,
  Validators
} from "@angular/forms";
import { IAdminCourse } from "../../interfaces/course";
import { Subject } from "rxjs";
import { takeUntil } from "rxjs/operators";
import { DockerImageApiService } from "../../../admin-docker-image/services/docker-image-api.service";
import { MatDialog } from "@angular/material/dialog";
import { DockerImageChoiceDialogComponent } from "../docker-image-choice-dialog/docker-image-choice-dialog.component";
import { IComputeType } from '../../../compute-type/interfaces/compute-type';
import { DefaultCourseFormValuesService } from "../../services/default-course-form-values.service";
import { IPort } from 'src/app/modules/ports-form/interfaces/port';

@Component({
  selector: 'app-course-technical-information-form',
  templateUrl: './course-technical-information-form.component.html',
  styleUrls: ['./course-technical-information-form.component.css'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => CourseTechnicalInformationFormComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => CourseTechnicalInformationFormComponent),
      multi: true
    }
  ]
})
export class CourseTechnicalInformationFormComponent implements OnInit, OnDestroy, ControlValueAccessor, Validator {
  readonly courseTechnicalForm: FormGroup;
  private readonly destroy$ = new Subject<void>();
  @Input() computeTypes: Array<IComputeType> = [];
  @Input() defaultComputeTypeId: number = 0;


  constructor(
    formBuilder: FormBuilder,
    private readonly dockerImageApiService: DockerImageApiService,
    private readonly dialog: MatDialog,
    @Inject(APP_CONFIG) readonly config: IAppConfig,
  ) {
    this.courseTechnicalForm = formBuilder.group({
      ports: [[]],

      dockerImage: ['', Validators.required],
      nanoCpusLimit: null,
      memoryBytesLimit: null,
      computeTypeId: null,
      command: null,

      saveStudentWork: false,
      workdirSize: null,
      workdirPath: null,
      allowStudentToSubmit: false,
      useStudentVolume: false,
      studentVolumePath: null,
      uid: null,

      displayOptions: formBuilder.control({}),
    }, {
      validators: [this.commandPortValidator]
    });
  }

  ngOnInit(): void {
    this.courseTechnicalForm.valueChanges
      .pipe(
        takeUntil(this.destroy$),
      )
      .subscribe(
        v => {
          v.nanoCpusLimit = v.nanoCpusLimit * 1e9;
          v.memoryBytesLimit = v.memoryBytesLimit * 1e9;
          this.propagateChange(v);
        }
      )
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {}

  setDisabledState(isDisabled: boolean): void {
    if (isDisabled) {
      this.courseTechnicalForm.disable();
    } else {
      this.courseTechnicalForm.enable();
    }
  }

  validate(control: AbstractControl): ValidationErrors | null {
    if (this.courseTechnicalForm.invalid) {
      return {courseTechnicalErrors: this.courseTechnicalForm.errors}
    }
    return null;
  }

  writeValue(obj: IAdminCourse): void {
    this.courseTechnicalForm.setValue(
      DefaultCourseFormValuesService.getTechnicalValues(
        obj, this.defaultComputeTypeId, true,
      ),
    );

    this.changeSaveStudentWork(obj?.saveStudentWork);
  }

  private propagateChange = (_: any) => {}

  changeSaveStudentWork(checked: boolean) {
    if(checked) {
      this.courseTechnicalForm.get('workdirSize')?.setValidators([Validators.required]);
      this.courseTechnicalForm.get('workdirPath')?.setValidators([Validators.required]);
    } else {
      this.courseTechnicalForm.get('workdirSize')?.clearValidators();
      this.courseTechnicalForm.get('workdirPath')?.clearValidators();
      this.courseTechnicalForm.get('allowStudentToSubmit')?.setValue(false);
    }
    this.courseTechnicalForm.get('workdirSize')?.updateValueAndValidity();
    this.courseTechnicalForm.get('workdirPath')?.updateValueAndValidity();
  }

  changeUseStudentVolume(checked: boolean) {
    if(checked) {
      this.courseTechnicalForm.get('studentVolumePath')?.setValidators([Validators.required]);
    } else {
      this.courseTechnicalForm.get('studentVolumePath')?.clearValidators();
    }
    this.courseTechnicalForm.get('studentVolumePath')?.updateValueAndValidity();
    this.courseTechnicalForm.get('uid')?.updateValueAndValidity();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
  }

  openDialog() {
    this.dialog.open(DockerImageChoiceDialogComponent).afterClosed()
    .subscribe(
      (data: any) => {
        if (data) {
          this.courseTechnicalForm.get('ports')?.setValue(data.dockerImage.ports);
          this.courseTechnicalForm.get('ports')?.updateValueAndValidity();
          this.courseTechnicalForm.get('dockerImage')?.setValue(data.dockerImageBuild.imageName);
          this.courseTechnicalForm.get('dockerImage')?.updateValueAndValidity();
        }
      }
    )
  }

  /**
   * Canonical grammar for the port placeholder a course may use in its launch command:
   *
   *     {{PORT['<digits>']}}   or   {{PORT["<digits>"]}}
   *
   * The opening and closing quotes must match and the body must be digits. The back-end
   * validator and the Go substitution implement the same grammar, so a change here belongs
   * in all three at once. Anything that opens with {{PORT[ and does not match is malformed:
   * the Go side leaves it in the command as a literal, so it is rejected here rather than
   * saved.
   */
  private static readonly COMMAND_PORT_PATTERN = /\{\{PORT\[(?:'(\d+)'|"(\d+)")\]\}\}/g;
  private static readonly MALFORMED_COMMAND_PORT_PATTERN = /\{\{PORT\[[^\]]*\]\}\}/g;

  commandPortValidator: ValidatorFn = (
    control: AbstractControl
  ): ValidationErrors | null => {
    const command: string = control.get('command')?.value;
    const ports: IPort[] = control.get('ports')?.value || [];

    if (!command || typeof command !== 'string') {
      return null;
    }

    const malformedPlaceholders: string[] = (
      command.match(CourseTechnicalInformationFormComponent.MALFORMED_COMMAND_PORT_PATTERN) || []
    ).filter(
      (placeholder) =>
        !new RegExp(
          `^${CourseTechnicalInformationFormComponent.COMMAND_PORT_PATTERN.source}$`
        ).test(placeholder)
    );

    if (malformedPlaceholders.length > 0) {
      return { malformedCommandPorts: { placeholders: malformedPlaceholders } };
    }

    const validPortKeys = new Set<string>();
    ports.forEach((p) => {
      if (p.mapPort != null) validPortKeys.add(p.mapPort.toString());
    });

    const portPattern = new RegExp(
      CourseTechnicalInformationFormComponent.COMMAND_PORT_PATTERN.source,
      'g'
    );
    const invalidPorts: string[] = [];
    let match: RegExpExecArray | null;
    while ((match = portPattern.exec(command)) !== null) {
      const extractedPort = match[1] ?? match[2];
      if (!validPortKeys.has(extractedPort)) {
        invalidPorts.push(extractedPort);
      }
    }

    return invalidPorts.length > 0
      ? { unknownCommandPorts: { invalidPorts } }
      : null;
  };
}

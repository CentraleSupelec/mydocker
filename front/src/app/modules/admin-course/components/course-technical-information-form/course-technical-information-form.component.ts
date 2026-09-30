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
   *     {{PORT['<port>']}}   or   {{PORT["<port>"]}}
   *
   * The opening and closing quotes must match and the body must be a port written without a
   * leading zero and no larger than 65535. The back-end validator and the Go substitution
   * implement the same grammar,
   * so a change here belongs in all three at once.
   *
   * Leading zeros are refused rather than normalised: the Go side would resolve '08080'
   * numerically to 8080 while this validator compares it as text against the declared ports
   * and sees an unknown one, so the three would disagree about the same command.
   *
   * Validation works by candidate rather than by shape: every occurrence of the opening
   * {{PORT[ is a candidate, and a candidate that is not a canonical placeholder starting at
   * that exact offset is malformed. Matching a "malformed shape" instead would miss the
   * unterminated cases, {{PORT[8080 and {{PORT['8080']} and a bare {{PORT[, all of which the
   * Go side leaves in the command as literals.
   */
  private static readonly COMMAND_PORT_PREFIX = '{{PORT[';
  private static readonly COMMAND_PORT_SOURCE = /\{\{PORT\[(?:'([1-9][0-9]*)'|"([1-9][0-9]*)")\]\}\}/.source;
  /** How much of a malformed candidate to quote back to the user. */
  private static readonly CANDIDATE_EXCERPT_LENGTH = 32;
  /**
   * The grammar bounds the port by value as well as by shape. Without this a course could save
   * {{PORT['4294975376']}}, which the Go side narrowed to a uint32 and resolved as 8080,
   * substituting a port nobody asked for.
   */
  private static readonly MAX_PORT = 65535;

  commandPortValidator: ValidatorFn = (
    control: AbstractControl
  ): ValidationErrors | null => {
    const command: string = control.get('command')?.value;
    const ports: IPort[] = control.get('ports')?.value || [];

    if (!command || typeof command !== 'string') {
      return null;
    }

    const prefix = CourseTechnicalInformationFormComponent.COMMAND_PORT_PREFIX;
    const anchored = new RegExp(
      CourseTechnicalInformationFormComponent.COMMAND_PORT_SOURCE,
      'y'
    );

    const malformedPlaceholders: string[] = [];
    const referencedPorts: string[] = [];

    for (
      let index = command.indexOf(prefix);
      index !== -1;
      index = command.indexOf(prefix, index + prefix.length)
    ) {
      anchored.lastIndex = index;
      const match = anchored.exec(command);
      const port = match === null ? null : match[1] ?? match[2];

      if (port !== null && Number(port) <= CourseTechnicalInformationFormComponent.MAX_PORT) {
        referencedPorts.push(port);
      } else {
        malformedPlaceholders.push(
          command.slice(
            index,
            index + CourseTechnicalInformationFormComponent.CANDIDATE_EXCERPT_LENGTH
          )
        );
      }
    }

    if (malformedPlaceholders.length > 0) {
      return { malformedCommandPorts: { placeholders: malformedPlaceholders } };
    }

    const validPortKeys = new Set<string>();
    ports.forEach((p) => {
      if (p.mapPort != null) validPortKeys.add(p.mapPort.toString());
    });

    const invalidPorts = referencedPorts.filter((port) => !validPortKeys.has(port));

    return invalidPorts.length > 0
      ? { unknownCommandPorts: { invalidPorts } }
      : null;
  };
}

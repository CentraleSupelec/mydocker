import { Component, forwardRef, Input, OnDestroy, OnInit } from '@angular/core';
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
import { IAdminCourse } from "../../interfaces/course";
import { Subject } from "rxjs";
import { takeUntil } from "rxjs/operators";
import { DockerImageApiService } from "../../../admin-docker-image/services/docker-image-api.service";
import { MatDialog } from "@angular/material/dialog";
import { DockerImageChoiceDialogComponent } from "../docker-image-choice-dialog/docker-image-choice-dialog.component";
import { IComputeType } from '../../../compute-type/interfaces/compute-type';

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

      displayOptions: formBuilder.control({}),
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
    this.courseTechnicalForm.setValue({
      ports: obj?.ports || [],

      dockerImage: obj?.dockerImage || '',
      nanoCpusLimit: obj?.nanoCpusLimit * 1e-9 || null,
      memoryBytesLimit: obj?.memoryBytesLimit * 1e-9 || null,
      computeTypeId: obj?.computeTypeId || this.defaultComputeTypeId,
      command: obj?.command || null,

      saveStudentWork: obj?.saveStudentWork || false,
      workdirSize: obj?.workdirSize || null,
      workdirPath: obj?.workdirPath || null,
      allowStudentToSubmit: obj?.allowStudentToSubmit || false,

      displayOptions: obj?.displayOptions || {},
      useStudentVolume: obj?.useStudentVolume || false,
      studentVolumePath: obj?.studentVolumePath || null,
    });

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
}

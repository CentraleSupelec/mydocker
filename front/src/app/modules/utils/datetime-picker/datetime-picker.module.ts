import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DatetimePickerComponent } from './datetime-picker/datetime-picker.component';
import { FlexLayoutModule } from '@angular/flex-layout';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { ReactiveFormsModule } from '@angular/forms';

@NgModule({
  declarations: [
    DatetimePickerComponent,
  ],
  imports: [
    CommonModule,
    FlexLayoutModule,
    MatFormFieldModule,
    MatInputModule,
    MatDatepickerModule,
    ReactiveFormsModule,
    MatIconModule,
  ],
  exports: [
    DatetimePickerComponent,
  ]
})
export class DatetimePickerModule {}

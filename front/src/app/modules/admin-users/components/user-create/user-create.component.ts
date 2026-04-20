import { Component } from '@angular/core';
import { FormBuilder, FormControl } from "@angular/forms";
import { UserApiService } from "../../service/user-api.service";
import { ObservableSnackNotificationService } from "../../../utils/snack-notification/observable-snack-notification.service";
import { SnackNotificationService } from "../../../utils/snack-notification/snack-notification.service";
import { Router } from "@angular/router";
import { TranslateService } from '@ngx-translate/core';

@Component({
  selector: 'app-user-create',
  templateUrl: './user-create.component.html',
  styleUrls: ['./user-create.component.css']
})
export class UserCreateComponent {
  readonly userForm: FormControl;

  constructor(
    formBuilder: FormBuilder,
    private readonly userApiService: UserApiService,
    private readonly toasterService: ObservableSnackNotificationService,
    private readonly toastService: SnackNotificationService,
    private readonly router: Router,
    private readonly translate: TranslateService
  ) {
    this.userForm = formBuilder.control({})
  }

  submit() {
    this.userApiService.createUser(this.userForm.value).subscribe(
      () => {
        this.toastService.push(this.translate.instant('admin.users_management.users.create_success'), 'success');
        this.router.navigate(['/admin/users']);
      },
      (err) => {
        if (err.status === 409) {
          this.toastService.push(this.translate.instant('admin.users_management.users.create_duplicate'), 'error');
          this.router.navigate(['/admin/users']);
        }
        else {
          this.toastService.push(this.translate.instant('admin.users_management.users.create_error'), 'error');
          console.error(err);
        }
      });
  }
}

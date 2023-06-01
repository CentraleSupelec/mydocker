import { Component } from '@angular/core';
import { FormBuilder, FormControl } from "@angular/forms";
import { UserApiService } from "../../service/user-api.service";
import { ObservableSnackNotificationService } from "../../../utils/snack-notification/observable-snack-notification.service";
import { SnackNotificationService } from "../../../utils/snack-notification/snack-notification.service";
import { Router } from "@angular/router";

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
  ) {
    this.userForm = formBuilder.control({})
  }

  submit() {
    this.userApiService.createUser(this.userForm.value).subscribe(
      () => {
        this.toastService.push("L'utilisateur a bien été crée", 'success');
        this.router.navigate(['/admin/users']);
      },
      (err) => {
        if (err.status === 409) {
          this.toastService.push("L'utilisateur existe déjà", 'error');
          this.router.navigate(['/admin/users']);
        }
        else {
          this.toastService.push("Erreur lors de la création de l'utilisateur", 'error');
          console.error(err);
        }
      });
  }
}

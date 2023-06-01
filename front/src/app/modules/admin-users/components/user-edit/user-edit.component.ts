import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormControl } from "@angular/forms";
import { UserApiService } from "../../service/user-api.service";
import { ObservableSnackNotificationService } from "../../../utils/snack-notification/observable-snack-notification.service";
import { ActivatedRoute } from "@angular/router";
import { IUser } from "../../../permissions/interfaces/user";

@Component({
  selector: 'app-user-edit',
  templateUrl: './user-edit.component.html',
  styleUrls: ['./user-edit.component.css']
})
export class UserEditComponent implements OnInit {
  readonly userForm: FormControl;
  user: IUser | null = null;

  constructor(
    formBuilder: FormBuilder,
    private readonly userApiService: UserApiService,
    private readonly toasterService: ObservableSnackNotificationService,
    private readonly route: ActivatedRoute,
  ) {
    this.userForm = formBuilder.control({})
  }

  ngOnInit(): void {
    this.route.data.subscribe(
      d => {
        this.user = d.user;
        this.userForm.setValue(this.user)
      }
    )
  }

  submit() {
    this.toasterService.toast(
      this.userApiService.editUser(this.user?.id, this.userForm.value),
      "L'utilisateur a bien été modifié",
      "Erreur lors de la modification de l'utilisateur",
      false,
      ['/admin/users'],
    )
  }
}

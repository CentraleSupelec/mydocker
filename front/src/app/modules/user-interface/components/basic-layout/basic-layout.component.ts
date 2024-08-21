import { Component } from '@angular/core';
import { AuthService } from "../../../authentication/services/auth.service";

@Component({
  templateUrl: './basic-layout.component.html',
  styleUrls: ['./basic-layout.component.scss']
})
export class BasicLayoutComponent {

  constructor(
    private readonly authService: AuthService,
  ) { }

  logout() {
    this.authService.signOut();
  }
}

import { Component, OnInit } from "@angular/core";
import { AuthService } from "../../../authentication/services/auth.service";
import { TokenService } from "../../../authentication/services/token.service";

@Component({
  templateUrl: "./basic-layout.component.html",
  styleUrls: ["./basic-layout.component.scss"],
})
export class BasicLayoutComponent implements OnInit {

  userInfo = '';
  email = '';

  constructor(
    private readonly authService: AuthService,
    protected readonly tokenService: TokenService,
  ) {
  }

  ngOnInit(): void {
    this.email = this.tokenService.getToken()?.decodedToken?.email ?? '';
    this.userInfo = `
        Username: ${this.tokenService.getToken()?.decodedToken.sub}
        Email: ${this.tokenService.getToken()?.decodedToken.email}
        `;
  }

  logout() {
    this.authService.signOut();
  }

}

import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from "@angular/router";
import { CasVerificationService } from "../../services/cas-verification.service";
import { mergeMap } from "rxjs/operators";
import { TokenService } from "../../services/token.service";

@Component({
  selector: 'app-login-accept',
  templateUrl: './login-accept.component.html',
  styleUrls: ['./login-accept.component.css']
})
export class LoginAcceptComponent implements OnInit {
  private redirectTo: string | null = null;

  constructor(
    private readonly route: ActivatedRoute,
    private readonly casVerificationService: CasVerificationService,
    private readonly tokenService: TokenService,
    private readonly router: Router,
  ) { }

  ngOnInit(): void {
    this.route.queryParamMap.pipe(
      mergeMap(paramMap => {
        this.redirectTo = paramMap.get('redirectTo');
        return this.casVerificationService.validateCasTicket(paramMap.get('ticket'), this.redirectTo)
      }),
      mergeMap(token => this.tokenService.loadToken(token))
    ).subscribe(
      () => this.router.navigateByUrl(decodeURIComponent(this.redirectTo || "/"))
    );
  }
}

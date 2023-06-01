import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { LtiApiService } from '../../services/lti-api.service';

@Component({
  selector: 'app-register',
  templateUrl: './register.component.html',
  styleUrls: ['./register.component.css']
})
export class RegisterComponent implements OnInit {
  message = 'Enregistrement...';
  constructor(
    private readonly route: ActivatedRoute,
    private readonly ltiApiService: LtiApiService
    ) { }

  ngOnInit(): void {
    this.route.queryParamMap.subscribe(queryParams => {
      if (!queryParams.has('openid_configuration') || !queryParams.has('registration_token')) {
        this.message = 'Paramètres manquants';
        return;
      }
      this.ltiApiService
        .register(queryParams.get('openid_configuration') as string, queryParams.get('registration_token') as string)
        .subscribe(() => {
          (window.opener || window.parent).postMessage({subject: 'org.imsglobal.lti.close'}, '*');
        }, error => {
          this.message = `Erreur : ${error.message}`;
        });
    });
  }

}

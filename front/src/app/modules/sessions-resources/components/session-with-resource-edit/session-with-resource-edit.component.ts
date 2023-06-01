import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from "@angular/router";
import { IResourceDescription, ISessionWithResources } from "../../interfaces/session-with-resources";
import { IOvhResource } from "../../interfaces/ovh-resource";
import { FormBuilder, FormControl } from "@angular/forms";
import { SessionWithResourcesApiService } from "../../services/session-with-resources-api.service";
import { ObservableSnackNotificationService } from "../../../utils/snack-notification/observable-snack-notification.service";


@Component({
  selector: 'app-session-with-resource-edit',
  templateUrl: './session-with-resource-edit.component.html',
  styleUrls: ['./session-with-resource-edit.component.css']
})
export class SessionWithResourceEditComponent implements OnInit {
  // @ts-ignore
  session: ISessionWithResources;
  resources: IOvhResource[] = [];
  ramSet: number = 0;
  ramRequired: number = 0
  cpuSet: number = 0;
  cpuRequired: number = 0;
  control: FormControl;


  constructor(
    formBuilder: FormBuilder,
    private readonly route: ActivatedRoute,
    private readonly sessionWithResourceApiService: SessionWithResourcesApiService,
    private readonly toasterService: ObservableSnackNotificationService,
  ) {
    this.control = formBuilder.control([]);
  }

  ngOnInit(): void {
    this.control.valueChanges.subscribe(
      (v: IResourceDescription[]) => {
        this.session.resources = v;
        this.ramSet = 0;
        this.cpuSet = 0
        v.forEach(
          resourceDescription => {
            const resource = this.resources.find(r => r.id === resourceDescription.ovhResourceId);
            if (resource) {
              this.ramSet += resource.ramInGo * resourceDescription.count;
              this.cpuSet += resource.coreNumber * resourceDescription.count;
            }
          }
        );
      }
    );

    this.route.data.subscribe(
      data => {
        this.session = data.session;
        this.resources = data.resources;
        this.ramRequired = this.session.course.memoryBytesLimit * this.session.studentNumber * 1e-9;
        this.cpuRequired = this.session.course.nanoCpusLimit * this.session.studentNumber * 1e-9;
        this.control.setValue(
          this.session.resources
        )
      }
    )
  }

  submit() {
    this.toasterService.toast(
      this.sessionWithResourceApiService.updateSession(
        this.session.id, this.session
      ),
      "Les ressources ont bien été mis à jour",
      "Erreur lors de la sauvegarde des ressources",
      false,
      ['/admin/sessions-resources']
    );
  }
}

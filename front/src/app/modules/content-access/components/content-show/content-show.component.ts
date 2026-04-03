import { Component, Inject, OnInit } from "@angular/core";
import { ActivatedRoute, Router } from "@angular/router";
import { APP_CONFIG, IAppConfig } from "src/app/app-config";
import { IContent } from "src/app/modules/content/interfaces/content";


@Component({
  selector: 'app-content-show',
  templateUrl: './content-show.component.html',
  styleUrls: ['./content-show.component.css']
})
export class ContentShowComponent implements OnInit {
  content?: IContent;

  constructor(
    @Inject(APP_CONFIG) readonly config: IAppConfig,
    private readonly route: ActivatedRoute,
    private readonly router: Router,
  ) {}

  ngOnInit(): void {
    this.route.data.subscribe(({content}) => {
      console.log(content);
      this.content = content
    })
  }
}

import { Component, OnInit } from '@angular/core';
import { ITerraformInstance } from "../../interfaces/terraform-instance";
import { ActivatedRoute } from "@angular/router";

interface IWorker {
  power_state: string;
  flavor_name: string;
  access_ip_v4: string;
  owner?: string;
}

@Component({
  selector: 'app-display-terraform-state',
  templateUrl: './display-terraform-state.component.html',
  styleUrls: ['./display-terraform-state.component.css']
})
export class DisplayTerraformStateComponent implements OnInit {
  workerByRegion: { [keys: string]: { [keys: string]: IWorker[] } } = {};
  regions: string[] = [];

  constructor(
    private readonly activatedRoute: ActivatedRoute
  ) {
  }

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(
      data => {
        data?.state?.forEach(
          (i: ITerraformInstance) => {
            if (!(i.attributes.region in this.workerByRegion)) {
              this.workerByRegion[i.attributes.region] = {}
              this.regions.push(i.attributes.region)
            }
            if (!(i.attributes.flavor_name in this.workerByRegion[i.attributes.region])) {
              this.workerByRegion[i.attributes.region][i.attributes.flavor_name] = []
            }
            this.workerByRegion[i.attributes.region][i.attributes.flavor_name].push(
              {
                power_state: i.attributes.power_state,
                flavor_name: i.attributes.flavor_name,
                access_ip_v4: i.attributes.access_ip_v4,
                owner: i.attributes.metadata?.owner,
              }
            )
          }
        )
      }
    )
  }

  flavorForRegion(region: string): string[] {
    return Object.keys(
      this.workerByRegion[region]
    );
  }
}

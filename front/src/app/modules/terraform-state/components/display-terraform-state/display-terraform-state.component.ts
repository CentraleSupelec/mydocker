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
  workerByClusterByRegion: { [keys: string] : { [keys: string]: { [keys: string]: IWorker[] } } } = {};
  regionsByCluster: { [keys: string] : string[] } = {};
  clusters: string[] = [];

  constructor(
    private readonly activatedRoute: ActivatedRoute
  ) {
  }

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(
      data => {
        (Object.entries(data?.state ?? {}) as [string, ITerraformInstance[]][])
          .forEach(([cluster, instances]) => {
            this.clusters.push(cluster);
            this.regionsByCluster[cluster] = [];
            this.workerByClusterByRegion[cluster] = {};
            instances.forEach((i) => {
              if (!(i.attributes.region in this.workerByClusterByRegion[cluster])) {
                this.workerByClusterByRegion[cluster][i.attributes.region] = {}
                this.regionsByCluster[cluster].push(i.attributes.region)
              }
              if (!(i.attributes.flavor_name in this.workerByClusterByRegion[cluster][i.attributes.region])) {
                this.workerByClusterByRegion[cluster][i.attributes.region][i.attributes.flavor_name] = []
              }
              this.workerByClusterByRegion[cluster][i.attributes.region][i.attributes.flavor_name].push(
                {
                  power_state: i.attributes.power_state,
                  flavor_name: i.attributes.flavor_name,
                  access_ip_v4: i.attributes.access_ip_v4,
                  owner: i.attributes.metadata?.owner,
                }
              )
            });
        });
      }
    )
  }

  flavorForClusterAndRegion(cluster: string, region: string): string[] {
    return Object.keys(
      this.workerByClusterByRegion[cluster][region]
    );
  }
}

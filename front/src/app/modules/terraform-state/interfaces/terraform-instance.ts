export interface ITerraformAttribute {
  region: string;
  power_state: string;
  flavor_name: string;
  access_ip_v4: string;
  metadata: {[key: string]: string};
}

export interface ITerraformInstance {
  index_key: string;
  attributes: ITerraformAttribute;
}

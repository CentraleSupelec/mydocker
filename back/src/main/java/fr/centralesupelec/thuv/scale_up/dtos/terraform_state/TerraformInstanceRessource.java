package fr.centralesupelec.thuv.scale_up.dtos.terraform_state;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("openstack_compute_instance_v2")
@JsonIgnoreProperties(ignoreUnknown = true)
public class TerraformInstanceRessource extends AbstractTerraformRessource {
    private List<TerraformInstance> instances;
}

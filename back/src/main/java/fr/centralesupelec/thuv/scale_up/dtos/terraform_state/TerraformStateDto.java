package fr.centralesupelec.thuv.scale_up.dtos.terraform_state;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TerraformStateDto {
    private Long version;
    private List<AbstractTerraformRessource> resources;
}

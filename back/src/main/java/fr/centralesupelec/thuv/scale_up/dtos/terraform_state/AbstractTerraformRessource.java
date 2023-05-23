package fr.centralesupelec.thuv.scale_up.dtos.terraform_state;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;

@Data
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        defaultImpl = OtherTerraformRessource.class,
        property = "type")
@JsonSubTypes(
        @JsonSubTypes.Type(value = TerraformInstanceRessource.class)
)
public class AbstractTerraformRessource {
}

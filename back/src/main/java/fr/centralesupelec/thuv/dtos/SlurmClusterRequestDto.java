package fr.centralesupelec.thuv.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

// Input DTO for creating/updating a cluster. displayName, technicalName and
// apiEndpoint are always required. secretKey is optional on update - a blank value keeps
// the stored secret - and required on create, enforced in the controller. The
// secret is write-only: it is never returned in any response. Its encrypted form must
// fit the 1024-character column: 512 ASCII characters encrypt to about 730.
@Data
public class SlurmClusterRequestDto {
    @NotBlank
    private String displayName;
    @NotBlank
    private String technicalName;
    @NotBlank
    private String apiEndpoint;
    @Size(max = 512)
    @Pattern(regexp = "\\p{ASCII}*", message = "must contain ASCII characters only")
    private String secretKey;
}

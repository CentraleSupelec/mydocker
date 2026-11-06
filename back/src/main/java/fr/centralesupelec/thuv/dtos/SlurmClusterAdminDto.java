package fr.centralesupelec.thuv.dtos;

import lombok.Data;

// Admin management projection: everything an admin needs to see in the cluster
// list EXCEPT the secret. The secret is write-only and never leaves the server
@Data
public class SlurmClusterAdminDto {
    private Long id;
    private String displayName;
    private String technicalName;
    private String apiEndpoint;
}

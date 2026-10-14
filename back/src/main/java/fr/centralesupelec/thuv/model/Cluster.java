package fr.centralesupelec.thuv.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Entity
@Table(name = "clusters")
@Data
public class Cluster {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotNull
    private String name;
    @NotNull
    private String goAddress;
    @NotNull
    private int goPort;
    private String terraformStateUrl;
    @NotNull
    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean deploymentEnabled = false;
}

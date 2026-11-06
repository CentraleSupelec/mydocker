package fr.centralesupelec.thuv.model;

import fr.centralesupelec.thuv.crypto.SlurmSecretKeyConverter;
import lombok.Data;
import lombok.EqualsAndHashCode;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "slurm_clusters", indexes = {
    @Index(columnList = "technicalName", unique = true)
})
@Data
@EqualsAndHashCode(of = {"id"})
public class SlurmCluster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(unique = true)
    private String displayName;

    @NotNull
    @Column(unique = true)
    private String technicalName;

    @NotNull
    @Column(unique = false)
    private String apiEndpoint;

    // Encrypted at rest via the converter. The stored value is
    // ciphertext, so the unique constraint no longer dedupes by plaintext; that
    // is acceptable since cluster secrets are expected to be distinct anyway.
    @NotNull
    @Column(unique = true, length = 1024)
    @Convert(converter = SlurmSecretKeyConverter.class)
    private String secretKey;
}

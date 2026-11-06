package fr.centralesupelec.thuv.dtos;

// Everything but the secret: listing clusters never loads, so never decrypts, a cluster secret.
public interface SlurmClusterView {
    Long getId();
    String getDisplayName();
    String getTechnicalName();
    String getApiEndpoint();
}

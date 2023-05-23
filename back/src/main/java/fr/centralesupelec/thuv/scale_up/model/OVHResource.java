package fr.centralesupelec.thuv.scale_up.model;

import lombok.Data;
import jakarta.persistence.*;

@Entity
@Table(name = "ovh_ressource")
@Data
public class OVHResource {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private Long ramInGo;

    @Column(nullable = false)
    private Long coreNumber;
}

package fr.centralesupelec.thuv.permissions.models;

import fr.centralesupelec.thuv.model.User;
import lombok.Data;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "permission")
@Data
@Inheritance
public abstract class Permission {
    public enum Type {
        edit, view
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id")
    @NotNull
    private User user;

    @Enumerated(EnumType.STRING)
    @NotNull
    private Permission.Type type;
}

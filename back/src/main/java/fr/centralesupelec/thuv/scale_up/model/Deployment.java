package fr.centralesupelec.thuv.scale_up.model;

import static java.lang.Integer.MAX_VALUE;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.Hibernate;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "deployment")
@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Inheritance
public class Deployment {
    public enum Status {
        OK, RUNNING, CREATED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = MAX_VALUE)
    private String description;

    @UpdateTimestamp
    private LocalDateTime updatedOn;

    @NotNull
    private LocalDateTime startDateTime;

    @Enumerated(EnumType.STRING)
    @NotNull
    private Deployment.Status status = Status.CREATED;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) {
            return false;
        }
        Deployment that = (Deployment) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}

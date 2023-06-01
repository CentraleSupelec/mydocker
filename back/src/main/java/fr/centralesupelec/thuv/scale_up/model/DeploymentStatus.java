package fr.centralesupelec.thuv.scale_up.model;

import static java.lang.Integer.MAX_VALUE;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.Hibernate;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "deployment_status")
@Getter
@Setter
@ToString
@RequiredArgsConstructor
public class DeploymentStatus {
    public enum Status {
        DONE, ERROR, RUNNING, CREATED, SKIPPED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = MAX_VALUE)
    private String logs;

    @Column(length = MAX_VALUE)
    private String buildErrors;

    @Column(length = MAX_VALUE)
    @NotNull
    private String workers;

    @NotNull
    @Enumerated(EnumType.STRING)
    private DeploymentStatus.Status status = Status.CREATED;

    @UpdateTimestamp
    private LocalDateTime updatedOn;

    @CreationTimestamp
    private LocalDateTime createdOn;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) {
            return false;
        }
        DeploymentStatus that = (DeploymentStatus) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}

package fr.centralesupelec.thuv.activity_logging.model;

import fr.centralesupelec.thuv.model.User;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "activity_log_records")
public class ActivityLogRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = true)
    private User user;

    @NotBlank
    @Column(nullable = false)
    private String userEmail;

    private String modelId;

    @Enumerated(EnumType.STRING)
    private LogModelName modelName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LogAction action;

    @CreationTimestamp
    private LocalDateTime createdOn;
}

package fr.centralesupelec.thuv.activity_logging.repository;

import fr.centralesupelec.thuv.activity_logging.model.ActivityLogRecord;
import fr.centralesupelec.thuv.activity_logging.model.LogAction;
import fr.centralesupelec.thuv.activity_logging.model.LogModelName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface LogRecordRepository extends JpaRepository<ActivityLogRecord, Long> {
    Optional<ActivityLogRecord> findFirstByUserIdAndModelIdAndModelNameAndActionInAndCreatedOnBeforeOrderByCreatedOnDesc(
            Long userId,
            String modelId,
            LogModelName modelName,
            List<LogAction> actions,
            LocalDateTime before
    );
}

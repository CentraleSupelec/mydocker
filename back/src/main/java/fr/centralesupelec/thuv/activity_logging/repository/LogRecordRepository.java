package fr.centralesupelec.thuv.activity_logging.repository;

import fr.centralesupelec.thuv.activity_logging.model.ActivityLogRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LogRecordRepository extends JpaRepository<ActivityLogRecord, Long> {
}

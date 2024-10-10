package fr.centralesupelec.thuv.activity_logging.services;

import fr.centralesupelec.thuv.activity_logging.model.ActivityLogRecord;
import fr.centralesupelec.thuv.activity_logging.model.LogAction;
import fr.centralesupelec.thuv.activity_logging.model.LogModelName;
import fr.centralesupelec.thuv.activity_logging.repository.LogRecordRepository;
import fr.centralesupelec.thuv.model.User;
import fr.centralesupelec.thuv.repository.UserRepository;
import fr.centralesupelec.thuv.security.MyUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@RequiredArgsConstructor
@Service
public class ActivityLogger {
    private final LogRecordRepository logRecordRepository;
    private final UserRepository userRepository;

    public void log(LogAction action, User user) {
        this.log(action, null, null, user);
    }

    public void log(LogAction action, LogModelName logModelName, String modelId) {
        MyUserDetails principal = (MyUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        this.log(action, logModelName, modelId, principal.getUserId().toString());
    }

    @Transactional
    public void log(LogAction action, LogModelName logModelName, String modelId, String userId) {
        User user = userRepository.getReferenceById(Long.parseLong(userId));
        this.log(action, logModelName, modelId, user);
    }

    public void log(LogAction action, LogModelName logModelName, String modelId, User user) {
        ActivityLogRecord record = new ActivityLogRecord()
                .setAction(action)
                .setUser(user)
                .setUserEmail(user.getUsername())
                .setModelName(logModelName)
                .setModelId(modelId)
                ;
        logRecordRepository.save(record);
    }
}

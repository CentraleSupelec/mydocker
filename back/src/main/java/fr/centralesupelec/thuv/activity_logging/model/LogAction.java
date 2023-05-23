package fr.centralesupelec.thuv.activity_logging.model;

public enum LogAction {
    USER_LOGIN_LTI,
    USER_LOGIN_CAS,
    ENVIRONMENT_ASK,
    ENVIRONMENT_CREATED_OK,
    ENVIRONMENT_CREATED_KO,
    ENVIRONMENT_DELAY_DELETION,
    ENVIRONMENT_SHUTDOWN,
    ENVIRONMENT_RESTART,
}

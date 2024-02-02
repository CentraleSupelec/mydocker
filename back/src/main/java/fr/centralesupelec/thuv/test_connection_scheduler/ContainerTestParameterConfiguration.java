package fr.centralesupelec.thuv.test_connection_scheduler;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@Validated
@ConfigurationProperties(prefix = "container-connection-test")
@Data
public class ContainerTestParameterConfiguration {
    private static final long DEFAULT_TIME_IN_SECOND_BETWEEN_TWO_CONNECTIONS_TRY = 5;
    private static final int DEFAULT_TIME_IN_SECOND_BEFORE_CONNECTION_TEST_TIMEOUT = 2;
    private static final long DEFAULT_NUMBER_OF_CONNECTION_RETRY = 45;
    private static final long DEFAULT_NUMBER_OF_CONNECTION_RETRY_FOR_GPU = 390;

    private long timeInSecondBetweenTwoConnectionsTry = DEFAULT_TIME_IN_SECOND_BETWEEN_TWO_CONNECTIONS_TRY;
    private int timeInSecondBeforeConnectionTestTimeout = DEFAULT_TIME_IN_SECOND_BEFORE_CONNECTION_TEST_TIMEOUT;
    private long numberOfConnectionRetry = DEFAULT_NUMBER_OF_CONNECTION_RETRY;
    private long numberOfConnectionRetryForGpu = DEFAULT_NUMBER_OF_CONNECTION_RETRY_FOR_GPU;
}

package fr.centralesupelec.thuv.service;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;

class ClusterCallErrorsTest {

    @Test
    void unavailableIsServiceUnavailable() {
        assertThat(ClusterCallErrors.toHttp(Status.UNAVAILABLE.asRuntimeException()))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
    }

    @Test
    void deadlineExceededIsServiceUnavailable() {
        assertThat(ClusterCallErrors.toHttp(Status.DEADLINE_EXCEEDED.asRuntimeException()))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
    }

    @Test
    void anyOtherStatusIsLeftAsIs() {
        StatusRuntimeException internal = Status.INTERNAL.asRuntimeException();

        assertThat(ClusterCallErrors.toHttp(internal)).isSameAs(internal);
    }
}

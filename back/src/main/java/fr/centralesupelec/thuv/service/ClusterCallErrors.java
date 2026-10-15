package fr.centralesupelec.thuv.service;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * What a client gets when a call to a cluster's Go API fails. A cluster that cannot be reached or
 * does not answer in time is a 503 the client can retry; any other gRPC status is a fault on our
 * side and stays a 500.
 */
final class ClusterCallErrors {
    private ClusterCallErrors() {
    }

    static RuntimeException toHttp(StatusRuntimeException e) {
        Status.Code code = e.getStatus().getCode();
        if (code == Status.Code.UNAVAILABLE || code == Status.Code.DEADLINE_EXCEEDED) {
            return new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Cluster unreachable, try again later",
                    e
            );
        }
        return e;
    }
}

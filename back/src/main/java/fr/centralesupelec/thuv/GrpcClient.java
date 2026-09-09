package fr.centralesupelec.thuv;

import io.grpc.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class GrpcClient {
    private static final Logger logger = LoggerFactory.getLogger(GrpcClient.class);
    private final String goAddress;
    private final int goPort;
    private final boolean debug;
    private final int maxInboundMessageSize;

    @Autowired
    public GrpcClient(
            @Value("${go.app.address}") String goAddress,
            @Value("${go.app.port}") int goPort,
            @Value("${go.debug}") boolean debug,
            @Value("${go.app.max-inbound-message-size}") int maxInboundMessageSize
    ) {
        this.goAddress = goAddress;
        this.goPort = goPort;
        this.debug = debug;
        this.maxInboundMessageSize = maxInboundMessageSize;
    }

    @Bean
    public ManagedChannel channel() {
        // Without this the limit is gRPC's own 4 MiB default, which nobody here chose and which
        // fails a whole call rather than degrading. Environment logs are the response that can
        // approach it, and they are also capped per task on the Go side.
        ManagedChannelBuilder<?> builder = ManagedChannelBuilder.forAddress(goAddress, goPort)
                .usePlaintext()
                .maxInboundMessageSize(maxInboundMessageSize);
        if (debug) {
            builder
                    .intercept(new ClientInterceptor() {
                        @Override
                        public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
                                MethodDescriptor<ReqT, RespT> methodDescriptor,
                                CallOptions callOptions,
                                Channel channel
                        ) {
                            logger.debug("{}  / {}", methodDescriptor.getFullMethodName(), callOptions.toString());
                            return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(
                                    channel.newCall(methodDescriptor, callOptions)
                            ) {
                                @Override
                                public void sendMessage(ReqT message) {
                                    if (logger.isTraceEnabled()) {
                                        Exception e = new Exception();
                                        logger.trace("Sending message to gRPC: {}\n{}", message, e.getStackTrace());
                                    } else {
                                        logger.debug("Sending message to gRPC: {}", message);
                                    }
                                    super.sendMessage(message);
                                }

                                @Override
                                public void start(Listener<RespT> responseListener, Metadata headers) {
                                    super.start(
                                            new ForwardingClientCallListener
                                                    .SimpleForwardingClientCallListener<RespT>(responseListener) {

                                                @Override
                                                public void onMessage(RespT message) {
                                                    logger.debug("Received message from gRPC: {}", message);
                                                    super.onMessage(message);
                                                }

                                            },
                                            headers
                                    );
                                }
                            };
                        }
                    });
        }
        return builder.build();
    }
}

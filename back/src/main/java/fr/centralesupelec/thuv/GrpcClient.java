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

    @Autowired
    public GrpcClient(
            @Value("${go.app.address}") String goAddress,
            @Value("${go.app.port}") int goPort,
            @Value("${go.debug}") boolean debug
    ) {
        this.goAddress = goAddress;
        this.goPort = goPort;
        this.debug = debug;
    }

    @Bean
    public ManagedChannel channel() {
        ManagedChannelBuilder<?> builder = ManagedChannelBuilder.forAddress(goAddress, goPort).usePlaintext();
        if (debug) {
            builder
                    .intercept(new ClientInterceptor() {
                        @Override
                        public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
                                MethodDescriptor<ReqT, RespT> methodDescriptor,
                                CallOptions callOptions,
                                Channel channel
                        ) {
                            logger.debug(String.format(
                                    "%s  / %s",
                                    methodDescriptor.getFullMethodName(),
                                    callOptions.toString()
                            ));
                            return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(
                                    channel.newCall(methodDescriptor, callOptions)
                            ) {
                                @Override
                                public void sendMessage(ReqT message) {
                                    logger.debug("Sending message to gRPC: {}", message);
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

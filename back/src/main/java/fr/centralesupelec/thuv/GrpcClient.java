package fr.centralesupelec.thuv;

import io.grpc.*;
import fr.centralesupelec.thuv.model.Cluster;
import fr.centralesupelec.thuv.repository.ClusterRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.net.ssl.SSLException;

@Configuration
public class GrpcClient {
    private static final Logger logger = LoggerFactory.getLogger(GrpcClient.class);
    private final boolean debug;
    private final int maxInboundMessageSize;
    private final ClusterRepository clusterRepository;

    @Autowired
    public GrpcClient(
            @Value("${go.debug}") boolean debug,
            @Value("${go.app.max-inbound-message-size}") int maxInboundMessageSize,
            ClusterRepository clusterRepository
    ) {
        this.debug = debug;
        this.maxInboundMessageSize = maxInboundMessageSize;
        this.clusterRepository = clusterRepository;
    }

    @Autowired
    private ChannelCredentials grpcTlsChannelCredentials;

    @Bean
    @Lazy
    public Map<String, ManagedChannel> channels() throws SSLException {
        // Without this the limit is gRPC's own 4 MiB default, which nobody here chose and which
        // fails a whole call rather than degrading. Environment logs are the response that can
        // approach it, and they are also capped per task on the Go side.
        List<Cluster> clusters = clusterRepository.findAll();
        Map<String, ManagedChannel> channels = new HashMap<>();

        for (Cluster cluster : clusters) {
            ManagedChannelBuilder<?> builder = Grpc.newChannelBuilderForAddress(
                cluster.getGoAddress(),
                cluster.getGoPort(),
                grpcTlsChannelCredentials
            )
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
            channels.put(cluster.getName(), builder.build());
        }


        return channels;
    }
}

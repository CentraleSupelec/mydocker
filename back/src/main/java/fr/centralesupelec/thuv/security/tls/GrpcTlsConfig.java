package fr.centralesupelec.thuv.security.tls;

import io.grpc.ChannelCredentials;
import io.grpc.TlsChannelCredentials;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

@Configuration
@RequiredArgsConstructor
public class GrpcTlsConfig {

    @Value("${grpc.tls.client-key-path}")
    private Resource clientKey;

    @Value("${grpc.tls.client-cert-path}")
    private Resource clientCert;

    @Value("${grpc.tls.ca-cert-path}")
    private Resource caCert;

    @Bean
    public ChannelCredentials grpcTlsChannelCredentials() throws Exception {
        return TlsChannelCredentials.newBuilder()
            .trustManager(caCert.getInputStream()).keyManager(
                clientCert.getInputStream(),
                clientKey.getInputStream())
            .build()
        ;
    }
}

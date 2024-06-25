package fr.centralesupelec.thuv.test_connection_scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509ExtendedTrustManager;
import java.io.IOException;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.List;

@Component
public class TestSocket {
    private static final Logger logger = LoggerFactory.getLogger(TestSocket.class);
    private static final int secondToMilliSecondRation = 1000;
    @Value("#{'${app.testConnectionScheduler.errorCodes}'.split(',')}")
    private List<Integer> errorCodes;
    @Value("${app.testConnectionScheduler.disableSslValidation}")
    private Boolean disableSslValidation;

    public static boolean isSocketAlive(String ipAddress, int port, int timeoutInSecond) {
        boolean isAlive = false;

        SocketAddress socketAddress = new InetSocketAddress(ipAddress, port);
        Socket socket = new Socket();

        logger.debug("Test connection for ip address: " + ipAddress + ", port: " + port);
        try {
            socket.connect(socketAddress, timeoutInSecond * secondToMilliSecondRation);
            socket.close();
            isAlive = true;
        } catch (IOException exception) {
            logger.debug(
                    "Unable to connect to " + ipAddress + ":" + port + ". " + exception.getMessage()
            );
        }
        return isAlive;
    }

    public boolean isHttpsAlive(String hostname, int timeoutInSecond) {
        logger.debug(String.format("Test HTTP connection for host '%s'", hostname));
        try {
            HttpRequest request = HttpRequest
                    .newBuilder()
                    .uri(new URI(String.format("https://%s", hostname)))
                    .GET()
                    .timeout(java.time.Duration.ofSeconds(timeoutInSecond))
                    .build();
            HttpClient.Builder clientBuilder = HttpClient.newBuilder();
            if (this.disableSslValidation) {
                clientBuilder.sslContext(this.getUnsecuredSSLContext());
            }
            HttpResponse<String> response = clientBuilder
                    .build()
                    .send(request, HttpResponse.BodyHandlers.ofString());
            return !this.errorCodes.contains(response.statusCode());
        } catch (URISyntaxException e) {
            logger.error(String.format("Unable to connect to %s : URI malformed", hostname), e);
            return false;
        } catch (IOException e) {
            logger.error(String.format("Unable to connect to %s : I/O Exception", hostname), e);
            return false;
        } catch (InterruptedException e) {
            logger.error(String.format("Unable to connect to %s : Interruption / Timeout", hostname), e);
            return false;
        }
    }

    protected SSLContext getUnsecuredSSLContext() {
        TrustManager trustManager = new X509ExtendedTrustManager() {
            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[]{};
            }
            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType) {
            }
            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType, Socket socket) {
            }
            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType, SSLEngine engine) {
            }
            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType) {
            }
            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType, Socket socket) {
            }
            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType, SSLEngine engine) {
            }
        };
        SSLContext sslContext;
        try {
            sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{trustManager}, new SecureRandom());
            return sslContext;
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new RuntimeException(e);
        }
    }
}

package fr.centralesupelec.thuv.test_connection_scheduler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Serves every case from local HTTPS servers with certificates made at test start,
 * so the result depends on the code only, never on the network.
 */
class TestSocketTest {
    private static final char[] PASSWORD = "test".toCharArray();
    private static final Integer timeout = 3;

    private static KeyPair caKeys;
    private static X509Certificate caCert;
    private static SSLContext trustingTestCa;
    private static HttpsServer validServer;
    private static HttpsServer expiredServer;
    private static HttpsServer wrongHostServer;

    @BeforeAll
    static void startServers() throws Exception {
        Instant now = Instant.now();
        caKeys = newKeyPair();
        caCert = sign("CN=TestSocketTest CA", caKeys, null, now.minus(Duration.ofDays(1)), now.plus(Duration.ofDays(1)));

        validServer = startServer("localhost", now.minus(Duration.ofDays(1)), now.plus(Duration.ofDays(1)));
        expiredServer = startServer("localhost", now.minus(Duration.ofDays(10)), now.minus(Duration.ofDays(1)));
        wrongHostServer = startServer("wrong.host.invalid", now.minus(Duration.ofDays(1)), now.plus(Duration.ofDays(1)));

        KeyStore trustStore = KeyStore.getInstance("PKCS12");
        trustStore.load(null, null);
        trustStore.setCertificateEntry("ca", caCert);
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(trustStore);
        trustingTestCa = SSLContext.getInstance("TLS");
        trustingTestCa.init(null, trustManagerFactory.getTrustManagers(), null);
    }

    @AfterAll
    static void stopServers() {
        validServer.stop(0);
        expiredServer.stop(0);
        wrongHostServer.stop(0);
    }

    @Test
    void isHttpsAliveWithVerification() {
        TestSocket testSocket = newTestSocket();
        ReflectionTestUtils.setField(testSocket, "disableSslValidation", false);
        ReflectionTestUtils.setField(testSocket, "errorCodes", List.of(404, 410, 503));
        assertTrue(testSocket.isHttpsAlive(host(validServer), timeout));
        assertTrue(testSocket.isHttpsAlive(host(validServer) + "/401", timeout));
        assertFalse(testSocket.isHttpsAlive(host(validServer) + "/404", timeout));
        assertFalse(testSocket.isHttpsAlive(host(validServer) + "/410", timeout));
        assertFalse(testSocket.isHttpsAlive(host(validServer) + "/503", timeout));
        assertFalse(testSocket.isHttpsAlive(host(expiredServer), timeout));
        assertFalse(testSocket.isHttpsAlive(host(wrongHostServer), timeout));
    }

    @Test
    void isHttpsAliveWithoutVerification() {
        TestSocket testSocket = newTestSocket();
        ReflectionTestUtils.setField(testSocket, "errorCodes", List.of(410, 503));
        ReflectionTestUtils.setField(testSocket, "disableSslValidation", true);
        assertTrue(testSocket.isHttpsAlive(host(validServer), timeout));
        assertTrue(testSocket.isHttpsAlive(host(expiredServer), timeout));
        assertTrue(testSocket.isHttpsAlive(host(wrongHostServer), timeout));
        assertTrue(testSocket.isHttpsAlive(host(validServer) + "/404", timeout));
        assertFalse(testSocket.isHttpsAlive(host(validServer) + "/410", timeout));
        assertFalse(testSocket.isHttpsAlive(host(validServer) + "/503", timeout));
    }

    // The tests above replace the verified context; this pins what production uses instead.
    @Test
    void verifiedContextIsTheJvmDefault() throws Exception {
        assertSame(SSLContext.getDefault(), new TestSocket().getVerifiedSSLContext());
    }

    private static TestSocket newTestSocket() {
        return new TestSocket() {
            @Override
            protected SSLContext getVerifiedSSLContext() {
                return trustingTestCa;
            }
        };
    }

    private static String host(HttpsServer server) {
        return "localhost:" + server.getAddress().getPort();
    }

    // Answers the status code named by the path ("/404" gives 404), and 200 on "/".
    private static HttpsServer startServer(String certificateHost, Instant notBefore, Instant notAfter) throws Exception {
        KeyPair keys = newKeyPair();
        X509Certificate cert = sign("CN=" + certificateHost, keys, certificateHost, notBefore, notAfter);

        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(null, null);
        keyStore.setKeyEntry("server", keys.getPrivate(), PASSWORD, new Certificate[]{cert, caCert});
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, PASSWORD);
        SSLContext serverContext = SSLContext.getInstance("TLS");
        serverContext.init(keyManagerFactory.getKeyManagers(), null, null);

        HttpsServer server = HttpsServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.setHttpsConfigurator(new HttpsConfigurator(serverContext));
        server.createContext("/", TestSocketTest::answer);
        server.start();
        return server;
    }

    private static void answer(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        int status = "/".equals(path) ? 200 : Integer.parseInt(path.substring(1));
        if (status == 401) {
            exchange.getResponseHeaders().add("WWW-Authenticate", "Basic realm=\"test\"");
        }
        exchange.sendResponseHeaders(status, -1);
        exchange.close();
    }

    private static KeyPair newKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    // Signs with the test CA; with no dnsName, makes the self-signed CA certificate itself.
    private static X509Certificate sign(String subject, KeyPair keys, String dnsName, Instant notBefore, Instant notAfter)
            throws Exception {
        boolean isCa = dnsName == null;
        X500Name issuer = isCa ? new X500Name(subject) : new X500Name(caCert.getSubjectX500Principal().getName());
        X509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
                issuer,
                BigInteger.valueOf(System.nanoTime()),
                Date.from(notBefore),
                Date.from(notAfter),
                new X500Name(subject),
                keys.getPublic()
        );
        if (isCa) {
            builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(true));
        } else {
            builder.addExtension(Extension.subjectAlternativeName, false,
                    new GeneralNames(new GeneralName(GeneralName.dNSName, dnsName)));
        }
        return new JcaX509CertificateConverter().getCertificate(
                builder.build(new JcaContentSignerBuilder("SHA256withRSA").build(isCa ? keys.getPrivate() : caKeys.getPrivate()))
        );
    }
}

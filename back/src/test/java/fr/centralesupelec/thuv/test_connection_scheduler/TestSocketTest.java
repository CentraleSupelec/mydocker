package fr.centralesupelec.thuv.test_connection_scheduler;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TestSocketTest {
    TestSocket testSocket;
    Integer timeout = 3;

    @Test
    void isHttpsAliveWithVerification() {
        testSocket = new TestSocket();
        ReflectionTestUtils.setField(testSocket, "disableSslValidation", false);
        ReflectionTestUtils.setField(testSocket, "errorCodes", List.of(404, 410, 503));
        assertTrue(testSocket.isHttpsAlive("badssl.com", timeout));
        assertTrue(testSocket.isHttpsAlive("httpstat.us/401", timeout));
        assertFalse(testSocket.isHttpsAlive("httpstat.us/404", timeout));
        assertFalse(testSocket.isHttpsAlive("httpstat.us/410", timeout));
        assertFalse(testSocket.isHttpsAlive("httpstat.us/503", timeout));
        assertFalse(testSocket.isHttpsAlive("expired.badssl.com", timeout));
        assertFalse(testSocket.isHttpsAlive("wrong.host.badssl.com", timeout));
    }

    @Test
    void isHttpsAliveWithoutVerification() {
        testSocket = new TestSocket();
        ReflectionTestUtils.setField(testSocket, "errorCodes", List.of(410, 503));
        ReflectionTestUtils.setField(testSocket, "disableSslValidation", true);
        assertTrue(testSocket.isHttpsAlive("badssl.com", timeout));
        assertTrue(testSocket.isHttpsAlive("expired.badssl.com", timeout));
        assertTrue(testSocket.isHttpsAlive("wrong.host.badssl.com", timeout));
        assertTrue(testSocket.isHttpsAlive("httpstat.us/404", timeout));
        assertFalse(testSocket.isHttpsAlive("httpstat.us/410", timeout));
        assertFalse(testSocket.isHttpsAlive("httpstat.us/503", timeout));
    }
}

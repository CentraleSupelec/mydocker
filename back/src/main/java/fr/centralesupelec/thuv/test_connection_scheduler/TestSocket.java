package fr.centralesupelec.thuv.test_connection_scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

public class TestSocket {
    private static final Logger logger = LoggerFactory.getLogger(TestSocket.class);
    private static final int secondToMilliSecondRation = 1000;

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
}

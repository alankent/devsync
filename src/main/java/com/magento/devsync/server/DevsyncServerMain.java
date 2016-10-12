package com.magento.devsync.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import com.magento.devsync.communications.Logger;

public class DevsyncServerMain {

    public static Logger logger = new Logger("S");

    public static void main(String[] args) throws IOException {

        String port;
        if (args.length > 0) {
            port = args[0];
        } else {
            port = System.getenv("DEVSYNC_PORT");
            if (port == null || port.length() == 0) {
                port = System.getProperty("devsync.port");
                if (port == null) {
                    System.out.println("Please supply port number to listen on.");
                    return;
                }
            }
        }

        int portNumber = Integer.parseInt(port);

        logger.log("Server listening on port " + portNumber);
        try (ServerSocket serverSocket = new ServerSocket(portNumber)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                logger.log("Accepted client connection request");

                // Spawn a new thread per socket connection.
                ServerConnection s = new ServerConnection(clientSocket, logger);
                new Thread(s, "Server-Socket").start();
            }
        }
    }
}

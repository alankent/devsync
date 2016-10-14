package com.magento.devsync;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import com.magento.devsync.communications.Logger;
import com.magento.devsync.server.ServerConnection;

public class DevsyncServerMain {

    public static Logger logger;

    public static void main(String[] args) throws IOException {

        String port = null;
        String templateDir = null;
        boolean quietMode = false;
        boolean debugMode = false;
        boolean verboseMode = false;
        
        int arg = 0;
        while (arg < args.length) {
            if (arg + 2 <= args.length && args[arg].equals("--port")) {
                port = args[arg + 1];
                arg += 2;
            } else if (arg + 2 <= args.length && args[arg].equals("--template")) {
                templateDir = args[arg + 1];
                arg += 2;
            } else if (args[arg].equals("--quiet")) {
                quietMode = true;
                arg++;
            } else if (args[arg].equals("--debug")) {
                debugMode = true;
                arg++;
            } else if (args[arg].equals("--verbose")) {
                verboseMode = true;
                arg++;
            } else {
                System.err.println("Usage: [ --port <port> ] [ --template <dir> ]");
                for (int i = 0; i < args.length; i++) {
                    System.err.println("  " + i + " " + args[i]);
                }
                System.exit(1);
            }
        }
        
        logger = new Logger("S", quietMode, debugMode, verboseMode);
        logger.info("DevSync server started.");
        
        if (port == null) {
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

        logger.info("Server listening on port " + portNumber);
        try (ServerSocket serverSocket = new ServerSocket(portNumber)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                logger.infoVerbose("Accepted client connection request");

                // Spawn a new thread per socket connection.
                ServerConnection s = new ServerConnection(clientSocket, logger, templateDir);
                Thread serverThread = new Thread(s, "Server-Socket");
                serverThread.setDaemon(true);
                serverThread.start();
            }
        }
    }
}

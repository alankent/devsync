package com.magento.devsync.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class DevsyncServerMain {

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
		
		System.out.println("Server listening on port " + portNumber);
		try (ServerSocket serverSocket = new ServerSocket(portNumber)) {
			while (true) {
				Socket clientSocket = serverSocket.accept();
				System.out.println("Accepted client connection request");
				Server.start(clientSocket);
			}
		}
	}
}

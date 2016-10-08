package com.magento.devsync.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class DevsyncServerMain {

	public static void main(String[] args) throws IOException {
		
		int portNumber = Integer.parseInt(args[0]);
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

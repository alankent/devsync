package com.magento.devsync.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class DevsyncServermMain {

	public static void main(String[] args) throws IOException {
		
		int portNumber = Integer.parseInt(args[0]);
		try (ServerSocket serverSocket = new ServerSocket(portNumber)) {
			while (true) {
				Socket clientSocket = serverSocket.accept();
				Thread t = new Thread(new Server());
				t.start();
			}
		}
	}

}

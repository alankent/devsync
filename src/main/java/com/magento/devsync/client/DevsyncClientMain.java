package com.magento.devsync.client;

import java.io.File;
import java.net.Socket;

import com.magento.devsync.communications.ProtocolSpec;
import com.magento.devsync.communications.SendMessage;
import com.magento.devsync.config.YamlFile;

public class DevsyncClientMain {
	
	public static final String VERSION = "0.1";

	public static void main(String[] args) {

		try {
			
			console("Devsync version " + VERSION + "\n");
			YamlFile config = YamlFile.readYaml(new File(".devsync.yml"));
			
			console("* Connecting to server\n");
			
			String host = System.getenv("DEVSYNC_HOST");
			if (host == null || host.equals("")) {
				console("Environment variable DEVSYNC_HOST is not set.");
				return;
			}
			
			String port = System.getenv("DEVSYNC_PORT");
			if (port == null || port.equals("")) {
				console("Environment variable DEVSYNC_PORT is not set.");
				return;
			}
			int portNum;
			try {
				portNum = Integer.parseInt(port);
			} catch (NumberFormatException e) {
				console("Environment variable DEVSYNC_PORT must be a number");
				return;
			}
			
			try (Socket sock = new Socket(host, portNum)) {
				ProtocolSpec c = new SendMessage(sock.getOutputStream());
				Client client = Client.start(c, sock.getInputStream(), config);
				
				console("* Starting client->server sync\n");
				client.clientToServerSync();
		
				console("* Starting server->client sync\n");
				client.runUntilSyncComplete();
				
				console("* Entering watch mode\n");
				client.watchMode();
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static final void console(String message) {
		System.out.print(message);
		System.out.flush();
	}
}

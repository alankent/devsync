package com.magento.devsync.client;

import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import com.magento.devsync.communications.ProtocolSpec;
import com.magento.devsync.communications.SendMessage;
import com.magento.devsync.config.YamlFile;
import com.magento.devsync.server.DevsyncServerMain;

public class DevsyncClientMain {
	
	public static final String VERSION = "0.1";

	public static void main(String[] args) {
		
		//TODO: CHEAT! Kick off server to make debugging easier.
		try {
			new Thread(new Runnable() { 
				public void run() {
					System.setOut(System.err);
					try {
						DevsyncServerMain.main(new String[] {"12345"});
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}).start();
			Thread.sleep(1);
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		try {
			
			console("Devsync version " + VERSION + "\n");
			byte[] encoded = Files.readAllBytes(Paths.get(".devsync.yml"));
			String ymlContents = new String(encoded, StandardCharsets.UTF_8);
			YamlFile config = YamlFile.parseYaml(ymlContents);
			
			console("* Connecting to server\n");
			
			String host = getHost();
			int portNum = getPort();
			
			try (Socket sock = new Socket(host, portNum)) {
				SendMessage c = new SendMessage(sock.getOutputStream());
				Client client = new Client(c, sock.getInputStream(), config);
				client.run();
			} catch (ConnectException e) {
				console("Failed to connect to " + host + ":" + portNum + ". " + e.getMessage());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static final void console(String message) {
		System.out.print(message);
		System.out.flush();
	}
	
	private static String getHost() {
		String host = System.getenv("DEVSYNC_HOST");
		if (host == null || host.equals("")) {
			host = System.getProperty("devsync.host");
			if (host == null || host.equals("")) {
				console("Environment variable DEVSYNC_HOST is not set.");
				System.exit(1);
			}
		}
		return host;
	}
	
	private static int getPort() {
		String port = System.getenv("DEVSYNC_PORT");
		if (port == null || port.equals("")) {
			port = System.getProperty("devsync.port");
			if (port == null || port.equals("")) {
				throw new RuntimeException("Environment variable DEVSYNC_PORT is not set.");
			}
		}
		try {
			return Integer.parseInt(port);
		} catch (NumberFormatException e) {
			throw new RuntimeException("Environment variable DEVSYNC_PORT must be a number");
		}
	}
}

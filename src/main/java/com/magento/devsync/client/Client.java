package com.magento.devsync.client;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

import com.magento.devsync.communications.ProtocolSpec;
import com.magento.devsync.communications.ReceiveMessage;
import com.magento.devsync.communications.SendMessage;
import com.magento.devsync.config.YamlFile;

public class Client {

	private SendMessage toServer;
	private InputStream fromServer;
	private YamlFile config;
	private ReceiveMessage listener;
	private ClientPathResolver clientPathResolver;
	private ClientFileSyncThread clientSyncThread;
	
	public Client(Socket sock, YamlFile config) {
		
		this.config = config;
		try {
			fromServer = sock.getInputStream();
			toServer = new SendMessage(sock.getOutputStream());
			clientPathResolver = new ClientPathResolver();
			listener = new ReceiveMessage(fromServer, toServer, this, config);
			clientSyncThread = new ClientFileSyncThread(toServer, fromServer, config, clientPathResolver);
		} catch (IOException e) {
			throw new RuntimeException("Failed to establish socket connection. " + e.getMessage());
		}
	}

	/**
	 * Main execution of client - listens for requests from the server
	 * which can come at any time, plus goes through the phases of file
	 * syncing.
	 */
	public void run() {
		
		// Send the protocol version (which will cause server to exit if its wrong).
		toServer.checkProtocolVersion(ProtocolSpec.PROTOCOL_VERSION);
		
		// Send configuration settings to the server.
		toServer.setConfig(config);
		
		// Use separate thread to do file system syncing (sending fingerprints and files to server).
		new Thread(clientSyncThread).start();
		
		// Current thread will read and process server requests.
		listener.run();
	}

	/**
	 * Called when server sends a message back saying it has finished its phase of file syncing.
	 */
	public void syncComplete() {
		clientSyncThread.syncComplete();
	}
}

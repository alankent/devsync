package com.magento.devsync.client;

import java.io.InputStream;

import com.magento.devsync.communications.ProtocolSpec;
import com.magento.devsync.communications.ReceiveMessage;
import com.magento.devsync.communications.SendMessage;
import com.magento.devsync.communications.SyncTreeWalker;
import com.magento.devsync.config.YamlFile;

public class Client {

	private SendMessage toServer;
	private InputStream fromServer;
	private YamlFile config;
	private ReceiveMessage listener;
	private ClientPathResolver clientPathResolver;
	private boolean startWatchMode = false;
	private Object lock = new Object();
	private ClientFileSyncThread clientSyncThread;
	
	public Client(SendMessage toServer, InputStream fromServer, YamlFile config) {
		
		this.toServer = toServer;
		this.fromServer = fromServer;
		this.config = config;
		this.clientPathResolver = new ClientPathResolver();
		
		listener = new ReceiveMessage(fromServer, toServer, this, config);
		clientSyncThread = new ClientFileSyncThread(toServer, fromServer, config, clientPathResolver);

	}

	/**
	 * Main execution of client - listens for requests from the server
	 * which can come at any time, plus goes through the phases of file
	 * syncing.
	 */
	public void run() {
		
		toServer.checkProtocolVersion(ProtocolSpec.PROTOCOL_VERSION);
		toServer.setMountPoints(config);
		
		// Use separate thread to do syncing.
		new Thread(clientSyncThread).start();
		
		// Current thread will read and process server requests.
		listener.run();
	}

	public void syncComplete() {
		clientSyncThread.syncComplete();
	}
}

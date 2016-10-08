package com.magento.devsync.client;

import java.io.InputStream;

import com.magento.devsync.communications.ProtocolSpec;
import com.magento.devsync.communications.ReceiveMessage;
import com.magento.devsync.communications.SyncTreeWalker;
import com.magento.devsync.config.YamlFile;

public class Client {

	private ProtocolSpec toServer;
	private InputStream fromServer;
	private YamlFile config;
	private ReceiveMessage listener;
	private Thread listenerThread;
	private ClientPathResolver clientPathResolver;
	
	private Client(ProtocolSpec toServer, InputStream fromServer, YamlFile config) {
		
		this.toServer = toServer;
		this.fromServer = fromServer;
		this.config = config;
		this.clientPathResolver = new ClientPathResolver();
		
		listener = new ReceiveMessage(fromServer, toServer, this);
		listenerThread = new Thread(listener);
		listenerThread.start();
	}

	/**
	 * Walk through all the mount points, then all inclusions that match
	 * the mount points, and walk the directory tree from those points,
	 * skipping exclusions. Send file and directory fingerprints to servers.
	 */
	public void clientToServerSync() {
		SyncTreeWalker walker = new SyncTreeWalker(toServer, config, clientPathResolver);
		walker.clientToServerWalk();
	}

	public void watchMode() {
		// TODO Auto-generated method stub
		//
	}

	public void runUntilSyncComplete() {
		// TODO Auto-generated method stub
		
	}

	public void syncComplete() {
		// TODO Auto-generated method stub
		
	}

	public static Client start(final ProtocolSpec toServer,
							   final InputStream fromServer,
							   final YamlFile config) {
		return new Client(toServer, fromServer, config);
	}
}

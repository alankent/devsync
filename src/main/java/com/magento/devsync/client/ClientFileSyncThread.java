package com.magento.devsync.client;

import java.io.InputStream;

import com.magento.devsync.communications.PathResolver;
import com.magento.devsync.communications.ReceiveMessage;
import com.magento.devsync.communications.SendMessage;
import com.magento.devsync.communications.SyncTreeWalker;
import com.magento.devsync.config.YamlFile;

/**
 * This goes through the various stages of the client.
 * (1) Copy files from client to server,
 * (2) Tell server to send updates from server to client,
 * (3) After server has finished, start watching the local file system
 */
public class ClientFileSyncThread implements Runnable {
	
	private SendMessage toServer;
	private InputStream fromServer;
	private YamlFile config;
	private boolean startWatchMode = false;
	private Object lock = new Object();
	private ClientPathResolver pathResolver;
	
	public ClientFileSyncThread(SendMessage toServer, InputStream fromServer, YamlFile config, ClientPathResolver pathResolver) {
		this.toServer = toServer;
		this.fromServer = fromServer;
		this.config = config;
		this.pathResolver = pathResolver;
	}

	@Override
	public void run() {
		
		// Step one, do a client side sync files to server.
		System.out.println("* Starting client->server sync");

		SyncTreeWalker walker = new SyncTreeWalker(toServer, config, pathResolver);
		walker.clientToServerWalk();				

		// Step two, tell server it can start its sync now
		System.out.println("* Starting server->client sync");
		toServer.initialSync();
		
		// Step 3, watch mode is triggered by SYNC-COMPLETE message coming from server.
		startWatchMode = false;
		synchronized (lock) {
			while (!startWatchMode) {
				try {
					lock.wait();
				} catch (InterruptedException e) {
					// Should not happen 
					return;
				}
			}
		}
		
		// Our thread can now do the file system watching stage.
		System.out.println("* Entering watch mode");
		toServer.watchList();
		
		//TODO: watchFileSystem();
	}
 
	/**
	 * Signal the client thread to go into file system watching mode.
	 */
	public void syncComplete() {
		synchronized (lock) {
			startWatchMode = true;
			lock.notifyAll();
		}
	}
}

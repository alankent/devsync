package com.magento.devsync.server;

import java.io.IOException;

import com.magento.devsync.communications.Logger;
import com.magento.devsync.communications.Requestor;
import com.magento.devsync.communications.PathResolver;
import com.magento.devsync.communications.SyncTreeWalker;
import com.magento.devsync.config.YamlFile;


public class ServerFileSync implements Runnable {
	
	private Logger logger;
	private Requestor master;
	private YamlFile config;
	private boolean serverSyncComplete = false;
	private Object lock = new Object();
	private PathResolver pathResolver;
	
	public ServerFileSync(Requestor toServer, YamlFile config, PathResolver pathResolver, Logger logger) {
		this.master = toServer;
		this.config = config;
		this.pathResolver = pathResolver;
		this.logger = logger;
	}

	@Override
	public void run() {
		
		// We are step 2, client has told us to do a sync
		logger.log("* Starting server->client sync");

		SyncTreeWalker walker = new SyncTreeWalker(master, config, pathResolver, logger);
		walker.serverToClientWalk();				

		// Step 3, tell client we are done. 
		try {
			master.syncComplete();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1); // TODO: System.exit(1) used to exit after fault.
		}
	}
 
	/**
	 * Signal the client thread to go into file system watching mode.
	 */
	public void syncComplete() {
		synchronized (lock) {
			serverSyncComplete = true;
			lock.notifyAll();
		}
	}
}

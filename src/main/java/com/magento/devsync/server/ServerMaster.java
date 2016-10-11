package com.magento.devsync.server;

import java.io.IOException;

import com.magento.devsync.client.ClientFileSync;
import com.magento.devsync.client.ClientFileWatcher;
import com.magento.devsync.client.ClientPathResolver;
import com.magento.devsync.communications.Channel;
import com.magento.devsync.communications.Logger;
import com.magento.devsync.communications.Requestor;
import com.magento.devsync.communications.PathResolver;
import com.magento.devsync.communications.SyncTreeWalker;
import com.magento.devsync.config.YamlFile;

public class ServerMaster implements Runnable {
	
	private Requestor master;
	private YamlFile config;
	private PathResolver pathResolver;
	private ServerFileSync serverSync;
	private ServerFileWatcher fileWatcher;
	private Logger logger;
	private boolean startServerFileSync = false;
	private Object lock = new Object();
	
	public ServerMaster(Channel channel, Logger logger) {
		this.logger = logger;
		master = new Requestor(channel, logger);
	}
	
	public void setConfig(YamlFile config, PathResolver pathResolver) {
		this.config = config;
		this.pathResolver = pathResolver;
		serverSync = new ServerFileSync(master, config, pathResolver, logger);
		fileWatcher = new ServerFileWatcher(config, logger);
	}

	@Override
	public void run() {
		
		// Wait for client to tell us to start doing a sync pass.
		synchronized (lock) {
			while (!startServerFileSync) {
				try {
					lock.wait();
				} catch (InterruptedException e) {
					// Ignore
				}
			}
		}

		// Starting server side file sync (server is master now).
		logger.log("* Server syncing initial changes to server");
		SyncTreeWalker walker = new SyncTreeWalker(master, config, pathResolver, logger);
		walker.serverToClientWalk();
		
		// Tell client server sync done, client now drives the next step.
		try {
			master.syncComplete();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1); // TODO: System.exit(1) used to exit after fault.
		}
		
		// Watch file system and send to server.
		logger.log("Listening for requests on server to send to client");
		fileWatcher.run();
	}

	/**
	 * This is called by the receiver thread.
	 * To avoid deadlocks, spawn a separate thread so caller can go back
	 * waiting for the next request in parallel.
	 * @throws IOException 
	 */
	public void initialSync() throws IOException {
		synchronized (lock) {
			startServerFileSync  = true;
			lock.notifyAll();
		}
	}
}

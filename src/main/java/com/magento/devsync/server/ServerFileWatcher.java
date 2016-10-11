package com.magento.devsync.server;

import com.magento.devsync.communications.Logger;
import com.magento.devsync.config.YamlFile;

public class ServerFileWatcher implements Runnable {

	private YamlFile config;
	private Logger logger;
	
	public ServerFileWatcher(YamlFile config, Logger logger) {
		this.config = config;
		this.logger = logger;
	}

	@Override
	public void run() {
		//TODO: Server file watcher class to be written
		logger.log("Server file watching starting (fake!)");
		try {
			Thread.sleep(1000000);
		} catch (InterruptedException e) {
			// Ignore
		}
	}

}

package com.magento.devsync.client;

import java.io.IOException;

import com.magento.devsync.communications.Channel;
import com.magento.devsync.communications.Logger;
import com.magento.devsync.communications.Requestor;
import com.magento.devsync.communications.ProtocolSpec;
import com.magento.devsync.config.YamlFile;

/**
 * This class drives the client conversation to the server process.
 */
public class ClientMaster implements Runnable {

	private Requestor requestor;
	private YamlFile config;
	private ClientPathResolver clientPathResolver;
	private ClientFileSync clientSync;
	private ClientFileWatcher fileWatcher;
	private Logger logger;
	
	public ClientMaster(Channel channel, YamlFile config, Logger logger) {
		
		this.config = config;
		this.logger = logger;
		requestor = new Requestor(channel, logger);
		clientPathResolver = new ClientPathResolver();
		clientSync = new ClientFileSync(requestor, config, clientPathResolver, logger);
		fileWatcher = new ClientFileWatcher();
	}

	/**
	 * Main execution of client - listens for requests from the server
	 * which can come at any time, plus goes through the phases of file
	 * syncing.
	 */
	public void run() {
		
		// Send the protocol version (which will cause server to exit if its wrong).
		logger.log("Checking protocol compatibility");
		try {
			if (!requestor.checkProtocolVersion(ProtocolSpec.PROTOCOL_VERSION)) {
				System.out.println("Incompatible version of client and server code");
				System.exit(1);
			}
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1); // TODO: System.exit(1) used to exit after fault.
		}
		
		// Send configuration settings to the server.
		logger.log("Setting configuration");
		try {
			if (!requestor.setConfig(config)) {
				System.out.println("Failed to configure server. Aborting");
				System.exit(1);
			}
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1); // TODO: System.exit(1) used to exit after fault.
		}
		
		// Perform the synchronization phase (client -> server and server -> client).
		// Returns only when both directions of initial file sync are complete.
		logger.log("Spawning initial file system sync");
		clientSync.run();
		
		// Watch file system and send to server.
		logger.log("Listening for requests for server");
		fileWatcher.run();
	}

	/**
	 * Called when server sends a message saying it has finished its phase of file syncing.
	 */
	public void syncComplete() {
		clientSync.syncComplete();
	}
}

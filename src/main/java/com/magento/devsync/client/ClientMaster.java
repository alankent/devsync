package com.magento.devsync.client;

import java.io.File;
import java.io.IOException;

import com.magento.devsync.communications.Channel;
import com.magento.devsync.communications.ConnectionLost;
import com.magento.devsync.communications.Logger;
import com.magento.devsync.communications.ProtocolSpec;
import com.magento.devsync.communications.Requestor;
import com.magento.devsync.config.YamlFile;
import com.magento.devsync.filewatcher.ModifiedFileHistory;

/**
 * This class drives the client conversation to the server process.
 */
public class ClientMaster {

    private Requestor requestor;
    private YamlFile config;
    private ClientPathResolver clientPathResolver;
    private ClientFileSync clientSync;
    private ClientFileWatcher fileWatcher;
    private Logger logger;
    private ModifiedFileHistory modifiedFileHistory;

    public ClientMaster(Channel channel, Logger logger, ModifiedFileHistory modifiedFileHistory) {

        this.logger = logger;
        this.modifiedFileHistory = modifiedFileHistory;
        requestor = new Requestor(channel, logger);
        clientPathResolver = new ClientPathResolver();
    }
    
    public void preConfigHandshake(boolean forceInitialization) throws ConnectionLost {
        
        // Send the protocol version (which will cause server to exit if its wrong).
        logger.debug("Checking protocol compatibility");
        try {
            if (!requestor.checkProtocolVersion(ProtocolSpec.PROTOCOL_VERSION)) {
                System.out.println("Incompatible version of client and server code");
                System.exit(1);
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1); // TODO: System.exit(1) used to exit after fault.
        }
        
        // If current directory empty, do initialization
        if (forceInitialization || new File(".").listFiles().length == 0) {
            logger.info("Performing new project initialization.");
            try {
                requestor.initializeProject();
            } catch (IOException e) {
                logger.warn(e);
                System.exit(1); // TODO: System.exit(1) used to exit after fault.
            }
        }
    }
    
    public void setConfig(YamlFile config) {
        this.config = config;
        clientSync = new ClientFileSync(requestor, config, clientPathResolver, logger);
        fileWatcher = new ClientFileWatcher(config, clientPathResolver, requestor, logger, modifiedFileHistory);
    }

    /**
     * Main execution of client - listens for requests from the server
     * which can come at any time, plus goes through the phases of file
     * syncing.
     * @throws ConnectionLost 
     */
    public void run() throws ConnectionLost {

        // Send configuration settings to the server.
        logger.infoVerbose("Setting configuration");
        try {
            if (!requestor.setConfig(config)) {
                logger.warn("Failed to configure server. Aborting");
                System.exit(1);
            }
        } catch (IOException e) {
            logger.warn(e);
            System.exit(1); // TODO: System.exit(1) used to exit after fault.
        }

        // Perform the synchronization phase (client -> server and server -> client).
        // Returns only when both directions of initial file sync are complete.
        logger.info("Performing pre-watch file system sync.");
        clientSync.run();

        // Watch file system and send to server.
        logger.info("Watching file system for updates.");
        fileWatcher.run();
    }

    /**
     * Called when server sends a message saying it has finished its phase of file syncing.
     */
    public void syncComplete(int fileSyncCount) {
        clientSync.syncComplete(fileSyncCount);
    }
}

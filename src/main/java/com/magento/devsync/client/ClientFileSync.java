package com.magento.devsync.client;

import java.io.IOException;

import com.magento.devsync.communications.Logger;
import com.magento.devsync.communications.Requestor;
import com.magento.devsync.communications.SyncTreeWalker;
import com.magento.devsync.config.YamlFile;

/**
 * This goes through the various stages of the client. (1) Copy files from
 * client to server, (2) Tell server to send updates from server to client, (3)
 * After server has finished, start watching the local file system
 */
public class ClientFileSync implements Runnable {

    private Logger logger;
    private Requestor requestor;
    private YamlFile config;
    private boolean serverSyncComplete = false;
    private Object lock = new Object();
    private ClientPathResolver pathResolver;

    public ClientFileSync(Requestor requestor, YamlFile config, ClientPathResolver pathResolver, Logger logger) {
        this.requestor = requestor;
        this.config = config;
        this.pathResolver = pathResolver;
        this.logger = logger;
    }

    @Override
    public void run() {

        // Step one, do a client side sync files to server.
        logger.infoVerbose("* Starting client->server sync");

        SyncTreeWalker walker = new SyncTreeWalker(requestor, config, pathResolver, logger);
        walker.clientToServerWalk();
        logger.infoVerbose("  Scanned " + walker.getSyncFileCount() + " local files.");

        // Step two, tell server it can start its sync now
        logger.infoVerbose("* Starting server->client sync");
        try {
            requestor.initialSync();
        } catch (IOException e) {
            logger.warn(e);
            System.exit(1); // TODO: System.exit(1) used to exit after fault.
        }

        // Make sure we don't exit (which would move us on to phase 3, watching
        // the file system) until the server tells us that it has finished.
        synchronized (lock) {
            while (!serverSyncComplete) {
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    // Should not happen
                }
            }
        }
    }

    /**
     * Signal the client thread to go into file system watching mode.
     */
    public void syncComplete(int fileSyncCount) {
        synchronized (lock) {
            serverSyncComplete = true;
            lock.notifyAll();
            logger.infoVerbose("  Scanned " + fileSyncCount + " remote files.");
        }
    }
}

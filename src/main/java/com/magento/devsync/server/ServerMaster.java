package com.magento.devsync.server;

import java.io.IOException;

import com.magento.devsync.communications.Channel;
import com.magento.devsync.communications.Logger;
import com.magento.devsync.communications.PathResolver;
import com.magento.devsync.communications.Requestor;
import com.magento.devsync.communications.SyncTreeWalker;
import com.magento.devsync.config.YamlFile;
import com.magento.devsync.filewatcher.ModifiedFileHistory;

public class ServerMaster implements Runnable {

    private Requestor requestor;
    private YamlFile config;
    private PathResolver pathResolver;
    private ServerFileWatcher fileWatcher;
    private Logger logger;
    private boolean startServerFileSync = false;
    private Object lock = new Object();
    private ModifiedFileHistory modifiedFileHistory;

    public ServerMaster(Channel channel, Logger logger, ModifiedFileHistory modifiedFileHistory) {
        this.logger = logger;
        this.modifiedFileHistory = modifiedFileHistory;
        requestor = new Requestor(channel, logger);
    }

    public void setConfig(YamlFile config, PathResolver pathResolver) {
        this.config = config;
        this.pathResolver = pathResolver;
        fileWatcher = new ServerFileWatcher(config, pathResolver, requestor, logger, modifiedFileHistory);
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
        SyncTreeWalker walker = new SyncTreeWalker(requestor, config, pathResolver, logger);
        walker.serverToClientWalk();

        // Tell client server sync done, client now drives the next step.
        try {
            requestor.syncComplete();
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

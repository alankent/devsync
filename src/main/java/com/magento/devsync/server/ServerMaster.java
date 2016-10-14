package com.magento.devsync.server;

import java.io.File;
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
    private String templateDir;

    public ServerMaster(Channel channel, Logger logger, ModifiedFileHistory modifiedFileHistory, String templateDir) {
        this.logger = logger;
        this.modifiedFileHistory = modifiedFileHistory;
        this.templateDir = templateDir;
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
        logger.infoVerbose("* Server syncing initial changes to server");
        SyncTreeWalker walker = new SyncTreeWalker(requestor, config, pathResolver, logger);
        walker.serverToClientWalk();

        // Tell client server sync done, client now drives the next step.
        try {
            requestor.syncComplete(walker.getSyncFileCount());
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1); // TODO: System.exit(1) used to exit after fault.
        }

        // Watch file system and send to server.
        logger.infoVerbose("Listening for requests on server to send to client");
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
    
    public void initializeProject() {
        if (templateDir == null) {
            return;
        }
        fileWalk(".");
    }
    
    private void fileWalk(String path) {
        File f = new File(PathResolver.joinPath(templateDir, path));
        if (!f.exists()) {
            logger.info("Project initilaization template directory does not exist: " + templateDir);
            return;
        }
        try {
            if (f.isDirectory()) {
                requestor.createDirectory(path);
                File[] files = f.listFiles();
                for (File child : files) {
                    fileWalk(path + "/" + child.getName());
                }
            } else {
                boolean canExecute = f.canExecute();
                requestor.writeFile(path, canExecute, f);
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1); // TODO: System.exit(1) used to exit after fault.
        }
    }
}

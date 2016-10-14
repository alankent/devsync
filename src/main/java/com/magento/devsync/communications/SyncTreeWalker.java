package com.magento.devsync.communications;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import com.magento.devsync.config.Mount;
import com.magento.devsync.config.SyncRule;
import com.magento.devsync.config.YamlFile;

public class SyncTreeWalker {

    private Logger logger; 
    private Requestor requestor;
    private YamlFile config;
    private PathResolver pathResolver;
    private int syncFileCount = 0;

    public SyncTreeWalker(
            Requestor requestor,
            YamlFile config,
            PathResolver pathResolver,
            Logger logger) {
        this.requestor = requestor;
        this.config = config;
        this.pathResolver = pathResolver;
        this.logger = logger;
    }

    public void clientToServerWalk() {
        for (Mount m : config.mounts) {
            for (List<SyncRule> syncRules : Arrays.asList(m.once, m.watch)) {
                for (SyncRule sr : syncRules) {
                    if (sr.mode.equals("push") || sr.mode.equals("sync")) {
                        fileWalk(PathResolver.joinPath(m.local, sr.path), sr.exclude);
                    }
                }
            }
        }
    }

    public void serverToClientWalk() {
        for (Mount m : config.mounts) {
            for (List<SyncRule> syncRules : Arrays.asList(m.once, m.watch)) {
                for (SyncRule sr : syncRules) {
                    if (sr.mode.equals("pull") || sr.mode.equals("sync")) {
                        fileWalk(PathResolver.joinPath(m.local, sr.path), sr.exclude);
                    }
                }
            }
        }
    }

    public void fileWalk(String path, List<String> exclude) {
        if (exclude.contains(path)) {
            return;
        }
        File f = pathResolver.clientPathToFile(path);
        if (!f.exists()) {
            logger.debugVerbose("Filewalk: Path does not exist: " + path + " => " + f);
            return;
        }
        logger.debugVerbose("FILEWALK: " + path + " => " + f);
        try {
            if (f.isDirectory()) {
                requestor.createDirectory(path);
                File[] files = f.listFiles();
                for (File child : files) {
                    fileWalk(path + "/" + child.getName(), exclude);
                }
            } else {
                String fingerprint = PathResolver.fingerprint(f);
                boolean canExecute = f.canExecute();
                requestor.pathFingerprint(path, canExecute, f, fingerprint);
                syncFileCount++;
            }
        } catch (IOException e) {
            logger.warn(e);
        }
    }
    
    public int getSyncFileCount() {
        return syncFileCount;
    }
}

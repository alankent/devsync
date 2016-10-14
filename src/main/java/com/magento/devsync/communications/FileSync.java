package com.magento.devsync.communications;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;

import com.magento.devsync.config.YamlFile;
import com.magento.devsync.filewatcher.FileWatcher;
import com.magento.devsync.filewatcher.FileWatcherListener;
import com.magento.devsync.filewatcher.ModifiedFileHistory;

public class FileSync {

    private YamlFile config;
    private PathResolver pathResolver;
    private Requestor requestor;
    private FileWatcher.Filter filter;
    private Logger logger;
    private ModifiedFileHistory modifiedFileHistory;

    public FileSync(YamlFile config, PathResolver pathResolver, Requestor requestor, FileWatcher.Filter filter, Logger logger, ModifiedFileHistory modifiedFileHistory) {
        this.config = config;
        this.pathResolver = pathResolver;
        this.requestor = requestor;
        this.filter = filter;
        this.logger = logger;
        this.modifiedFileHistory = modifiedFileHistory;
    }

    public void run() throws ConnectionLost {
        FileWatcher watcher;
        try {
            watcher = new FileWatcher(config, pathResolver, new FileSyncListener(), filter, modifiedFileHistory, logger);
            watcher.run();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private final class FileSyncListener implements FileWatcherListener {

        @Override
        public void fileDeleted(String path) throws ConnectionLost {
            try {
                logger.infoVerbose("Removing: " + path);
                requestor.pathDeleted(path);
            } catch (IOException e) {
                throw new ConnectionLost(e);
            }
        }

        @Override
        public void fileChanged(String path) throws ConnectionLost {
            try {
                File f = pathResolver.clientPathToFile(path);
                if (modifiedFileHistory.beingWrittenTo(path)) {
                    // Ignore this file - *we* are writing to it!
                } else if (modifiedFileHistory.contains(path)) {
                    // If we wrote this file recently, the file system modified trigger
                    // might have been ourselves. So do a 'write if modified' sequence
                    // instead of a 'definitely write this file' sequence.
                    logger.infoVerbose("Write-if-changed: " + f);
                    requestor.pathFingerprint(path, f.canExecute(), f, PathResolver.fingerprint(f));
                } else {
                    logger.infoVerbose("Writing: " + f);
                    requestor.writeFile(path, f.canExecute(), f);
                }
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        @Override
        public void directoryDeleted(String path) throws ConnectionLost {
            try {
                logger.infoVerbose("Removing: " + path);
                requestor.pathDeleted(path);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        @Override
        public void directoryCreated(String path) throws ConnectionLost {
            try {
                logger.infoVerbose("Creating: " + path);
                requestor.createDirectory(path);

                // A directory rename comes through as delete and create,
                // so walk the 'new' directory to see if it contains
                // files we need to copy.
                walkNewDirectoryTree(path);

            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        private void walkNewDirectoryTree(String path) throws IOException, ConnectionLost {
            for (File f : pathResolver.clientPathToFile(path).listFiles()) {
                String child = PathResolver.joinPath(path, f.getName());
                if (Files.isDirectory(f.toPath(), LinkOption.NOFOLLOW_LINKS)) {
                    requestor.createDirectory(child);
                    walkNewDirectoryTree(child);
                } else {
                    requestor.writeFile(child, f.canExecute(), f);
                }
            }
        }
    }
}

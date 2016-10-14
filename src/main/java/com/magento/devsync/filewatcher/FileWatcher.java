package com.magento.devsync.filewatcher;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.magento.devsync.communications.ConnectionLost;
import com.magento.devsync.communications.Logger;
import com.magento.devsync.communications.PathResolver;
import com.magento.devsync.config.Mount;
import com.magento.devsync.config.SyncRule;
import com.magento.devsync.config.YamlFile;

public class FileWatcher {

    private YamlFile config;
    private PathResolver pathResolver;
    private WatchService watchService;
    private Set<String> ignorePaths = new HashSet<>();
    private Map<WatchKey,String> pathsByKey = new HashMap<>();
    private FileWatcherListener listener;
    private Filter filter;
    private Logger logger;
    private ModifiedFileHistory history;
    private String bufferedFileNotification;

    public static interface Filter {
        /**
         * Returns null if should not be synced.
         */
        String path(Mount mount, SyncRule syncRule);
    }

    public FileWatcher(YamlFile config, PathResolver pathResolver, FileWatcherListener listener, Filter filter, ModifiedFileHistory history, Logger logger) throws IOException {
        this.config = config;
        this.pathResolver = pathResolver;
        this.listener = listener;
        this.filter = filter;
        this.history = history;
        this.logger = logger;

        // Get the watch service.
        watchService = FileSystems.getDefault().newWatchService();
    }

    public void run() throws ConnectionLost {
        
        // Register listeners
        for (Mount m : config.mounts) {
            for (SyncRule sr : m.watch) {

                String clientPath = filter.path(m, sr);
                if (clientPath != null) {

                    // Work out the paths to ignore.
                    for (String exclude : sr.exclude) {
                        logger.debug("FileWatcher: excluding " + PathResolver.joinPath(clientPath, exclude));
                        ignorePaths.add(PathResolver.joinPath(clientPath, exclude));
                    }

                    // Register the watchers.
                    try {
                        logger.debug("FileWatcher: registering " + clientPath);
                        registerRecursive(clientPath);
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
        }

        // Loop, getting events
        processEvents();

        // Remove listeners (clean up)
        try {
            watchService.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void registerRecursive(String clientPath) throws IOException {

        if (ignorePaths.contains(clientPath)) {
            return;
        }

        File f = pathResolver.clientPathToFile(clientPath);
        if (!f.exists()) {
            logger.infoVerbose("Warning: Asked to watch non-existing directory (ignoring) - " + clientPath);
            return;
        }
        WatchKey key = f.toPath().register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY);
        pathsByKey.put(key, clientPath);

        if (f.isDirectory()) {
            for (File child : f.listFiles()) {
                if (child.isDirectory()) {
                    registerRecursive(PathResolver.joinPath(clientPath, child.getName()));
                }
            }
        }
    }

    /**
     * Process all events for keys queued to the watcher
     * @throws ConnectionLost 
     */
    void processEvents() throws ConnectionLost {
        
        bufferedFileNotification = null;

        // Loop forever.
        while (true) {

            // wait for key to be signalled
            WatchKey key = null;
            try {
                // Merge consecutive events for the same file.
                if (bufferedFileNotification == null) {
                    // If nothing buffered, just block waiting for event
                    logger.debugVerbose("Waiting for next FS change.");
                    key = watchService.take();
                } else {
                    // If something in buffer, poll for a short period.
                    logger.debugVerbose("Polling for next FS change.");
                    key = watchService.poll(500, TimeUnit.MILLISECONDS);
                    if (key == null) {
                        // Nothing more came through, flush the buffer
                        // and block waiting for next file change.
                        logger.debugVerbose("FS poll timed out.");
                        flushBuffer();
                        logger.debugVerbose("Blocking for next FS change.");
                        key = watchService.take();
                    }
                }
                logger.debugVerbose("Got a watch key!");
                history.removeExpiredEntries();
            } catch (InterruptedException x) {
                logger.debugVerbose("Exiting watch loop due to interrupt");
                return;
            }

            String clientPath = pathsByKey.get(key);
            if (clientPath == null) {
                logger.debugVerbose("WatchKey not recognized!!");
                continue;
            }
            logger.debugVerbose("Triggered by " + clientPath);

            for (WatchEvent<?> event: key.pollEvents()) {
                WatchEvent.Kind<?> kind = event.kind();

                // Oops, did we lose some events?
                if (kind == StandardWatchEventKinds.OVERFLOW) {
                    logger.debugVerbose("Warning - some events were lost"); // TODO
                    continue;
                }

                // Context for directory entry event is the file name of entry
                WatchEvent<Path> ev = cast(event);
                Path name = ev.context();
                String child = PathResolver.joinPath(clientPath, name.getFileName().toString());

                // If directory is created, start watching it too.
                if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
                    try {
                        if (Files.isDirectory(Paths.get(child), LinkOption.NOFOLLOW_LINKS)) {
                            registerRecursive(child);
                        }
                    } catch (IOException x) {
                        // TODO
                    }
                }

                // Run through exclusion list (directory contents will be excluded already, but
                // individual files or the directory itself will come through because of the parent).
                if (!ignorePaths.contains(child)) {

                    // Process the event
                    if (Files.isDirectory(Paths.get(child), LinkOption.NOFOLLOW_LINKS)) {
                        flushBuffer();
                        if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
                            listener.directoryCreated(child);
                        } else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
                            listener.directoryDeleted(child);
                        } else if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                            //TODO: Not sure what to do with this...
                        }
                    } else {
                        // Its a plain file.
                        if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
                            if (bufferedFileNotification != null
                                    && bufferedFileNotification.equals(child)) {
                                logger.debugVerbose("Not writing file to be deleted: " + bufferedFileNotification);
                                bufferedFileNotification = null;
                            }
                            flushBuffer();
                            listener.fileDeleted(child);
                        } else if (kind == StandardWatchEventKinds.ENTRY_CREATE
                                || kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                            if (bufferedFileNotification != null) {
                                // Flush if different, drop old event if same as
                                // new event.
                                if (!bufferedFileNotification.equals(child)) {
                                    logger.debugVerbose("Buffered file " + bufferedFileNotification + " not same as " + child);
                                    flushBuffer();
                                } else {
                                    // Do nothing, causing old event to be discarded
                                    logger.debugVerbose("Merging events for buffered file " + bufferedFileNotification);
                                }
                            }
                            logger.debugVerbose("Buffer set to " + child);
                            bufferedFileNotification = child;
                        }
                    }
                }
            }

            // reset key and remove from set if directory no longer accessible
            boolean valid = key.reset();
            if (!valid) {
                pathsByKey.remove(key);

                // all directories are inaccessible
                if (pathsByKey.isEmpty()) {
                    break;
                }
            }
        }
    }
    
    private void flushBuffer() throws ConnectionLost {
        if (bufferedFileNotification != null) {
            logger.debugVerbose("Flushing buffered file change: " + bufferedFileNotification);
            listener.fileChanged(bufferedFileNotification);
            bufferedFileNotification = null;
        }
    }

    @SuppressWarnings("unchecked")
    private WatchEvent<Path> cast(WatchEvent<?> event) {
        return (WatchEvent<Path>) event;
    }
}

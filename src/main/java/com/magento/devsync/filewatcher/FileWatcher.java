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

import com.magento.devsync.communications.Logger;
import com.magento.devsync.communications.PathResolver;
import com.magento.devsync.config.Mount;
import com.magento.devsync.config.SyncRule;
import com.magento.devsync.config.YamlFile;

public class FileWatcher implements Runnable {

    private YamlFile config;
    private PathResolver pathResolver;
    private WatchService watchService;
    private Set<String> ignorePaths = new HashSet<>();
    private Map<WatchKey,String> pathsByKey = new HashMap<>();
    private FileWatcherListener listener;
    private Filter filter;
    private Logger logger;

    public static interface Filter {
        /**
         * Returns null if should not be synced.
         */
        String path(Mount mount, SyncRule syncRule);
    }

    public FileWatcher(YamlFile config, PathResolver pathResolver, FileWatcherListener listener, Filter filter, Logger logger) throws IOException {
        this.config = config;
        this.pathResolver = pathResolver;
        this.listener = listener;
        this.filter = filter;
        this.logger = logger;

        // Get the watch service.
        watchService = FileSystems.getDefault().newWatchService();
    }

    @Override
    public void run() {

        // Register listeners
        for (Mount m : config.mounts) {
            for (SyncRule sr : m.watch) {

                String clientPath = filter.path(m, sr);
                if (clientPath != null) {

                    // Work out the paths to ignore.
                    for (String exclude : sr.exclude) {
                        logger.log("FileWatcher: excluding " + PathResolver.joinPath(clientPath, exclude));
                        ignorePaths.add(PathResolver.joinPath(clientPath, exclude));
                    }

                    // Register the watchers.
                    try {
                        logger.log("FileWatcher: registering " + clientPath);
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
     */
    void processEvents() {

        // Loop forever.
        while (true) {

            // wait for key to be signalled
            WatchKey key;
            try {
                key = watchService.take();
                logger.log("Got a watch key!");

            } catch (InterruptedException x) {
                logger.log("Exiting watch loop due to interrupt");
                return;
            }

            String clientPath = pathsByKey.get(key);
            if (clientPath == null) {
                System.err.println("WatchKey not recognized!!");
                continue;
            }
            logger.log("Triggered by " + clientPath);

            for (WatchEvent<?> event: key.pollEvents()) {
                WatchEvent.Kind<?> kind = event.kind();

                // Oops, did we lose some events?
                if (kind == StandardWatchEventKinds.OVERFLOW) {
                    System.err.println("Warning - some events were lost"); // TODO
                    continue;
                }

                // Context for directory entry event is the file name of entry
                WatchEvent<Path> ev = (WatchEvent<Path>) event;
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
                        if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
                            listener.directoryCreated(child);
                        } else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
                            listener.directoryDeleted(child);
                        } else if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                            //TODO: Not sure what to do with this...
                        }
                    } else {
                        // Its a plain file.
                        if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
                            listener.fileChanged(child);
                        } else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
                            listener.fileDeleted(child);
                        } else if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                            listener.fileChanged(child);
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
}

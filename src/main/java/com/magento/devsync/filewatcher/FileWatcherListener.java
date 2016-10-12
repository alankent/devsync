package com.magento.devsync.filewatcher;

public interface FileWatcherListener {

    void fileChanged(String path);
    void fileDeleted(String path);
    void directoryCreated(String path);
    void directoryDeleted(String path);

}

package com.magento.devsync.filewatcher;

import com.magento.devsync.communications.ConnectionLost;

public interface FileWatcherListener {

    void fileChanged(String path) throws ConnectionLost;
    void fileDeleted(String path) throws ConnectionLost;
    void directoryCreated(String path) throws ConnectionLost;
    void directoryDeleted(String path) throws ConnectionLost;

}

package com.magento.devsync.client;

import com.magento.devsync.communications.FileSync;
import com.magento.devsync.communications.Logger;
import com.magento.devsync.communications.PathResolver;
import com.magento.devsync.communications.Requestor;
import com.magento.devsync.config.Mount;
import com.magento.devsync.config.SyncRule;
import com.magento.devsync.config.YamlFile;
import com.magento.devsync.filewatcher.ModifiedFileHistory;
import com.magento.devsync.filewatcher.FileWatcher.Filter;

public class ClientFileWatcher implements Runnable {

	private FileSync fileSync;
	
	public ClientFileWatcher(YamlFile config, PathResolver pathResolver, Requestor requestor, Logger logger, ModifiedFileHistory modifiedFileHistory) {
		fileSync = new FileSync(config, pathResolver, requestor, new Filter() {
			@Override
			public String path(Mount mount, SyncRule syncRule) {
				if (syncRule.mode.equals("push") || syncRule.mode.equals("sync")) {
					return PathResolver.joinPath(mount.local, syncRule.path);
				}
				return null;
			}
		}, logger, modifiedFileHistory);
	}
	
	@Override
	public void run() {
		fileSync.run();
	}

}

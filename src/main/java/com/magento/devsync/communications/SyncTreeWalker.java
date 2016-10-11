package com.magento.devsync.communications;

import java.io.File;
import java.io.IOException;
import java.util.List;

import com.magento.devsync.config.Mount;
import com.magento.devsync.config.SyncRule;
import com.magento.devsync.config.YamlFile;

public class SyncTreeWalker {

	private Logger logger; 
	private Requestor master;
	private YamlFile config;
	private PathResolver pathResolver;
	
	public SyncTreeWalker(
			Requestor master,
			YamlFile config,
			PathResolver pathResolver,
			Logger logger) {
		this.master = master;
		this.config = config;
		this.pathResolver = pathResolver;
		this.logger = logger;
	}
	
	public void clientToServerWalk() {
		for (Mount m : config.mounts) {
			for (SyncRule sr : m.once) {
				if (sr.mode.equals("push") || sr.mode.equals("sync")) {
					fileWalk(PathResolver.path(m, sr), sr.exclude);
				}
			}
		}
	}
	
	public void serverToClientWalk() {
		for (Mount m : config.mounts) {
			for (SyncRule sr : m.once) {
				if (sr.mode.equals("pull") || sr.mode.equals("sync")) {
					fileWalk(PathResolver.path(m, sr), sr.exclude);
				}
			}
		}
	}
	
	public void fileWalk(String path, List<String> exclude) {
		if (exclude.contains(path)) {
			return;
		}
		File f = pathResolver.localPath(path);
		if (!f.exists()) {
			logger.log("Filewalk: Path does not exist: " + path + " => " + f);
			return;
		}
		logger.log("FILEWALK: " + path + " => " + f);
		String fingerprint = PathResolver.fingerprint(f);
		try {
			boolean canExecute = f.canExecute();
			master.pathFingerprint(path, canExecute, f, fingerprint);
			if (f.isDirectory()) {
				File[] files = f.listFiles();
				for (File child : files) {
					fileWalk(path + "/" + child.getName(), exclude);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1); // TODO: System.exit(1) used to exit after fault.
		}
	}
}

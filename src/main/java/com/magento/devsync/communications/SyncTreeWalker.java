package com.magento.devsync.communications;

import java.io.File;
import java.util.List;

import com.magento.devsync.client.ClientPathResolver;
import com.magento.devsync.config.Mount;
import com.magento.devsync.config.SyncRule;
import com.magento.devsync.config.YamlFile;

public class SyncTreeWalker {

	private ProtocolSpec toServer;
	private YamlFile config;
	private PathResolver pathResolver;
	
	public SyncTreeWalker(
			ProtocolSpec toServer,
			YamlFile config,
			PathResolver pathResolver) {
		this.toServer = toServer;
		this.config = config;
		this.pathResolver = pathResolver;
	}
	
	public void clientToServerWalk() {
		for (Mount m : config.mounts) {
			for (SyncRule sr : m.once) {
				if (sr.mode.equals("push") || sr.mode.equals("sync")) {
					fileWalk(sr.path, sr.exclude);
				}
			}
		}
	}
	
	public void serverToClientWalk() {
		if (config == null) System.out.println("config null");
		if (config.mounts == null) System.out.println("config.mounts null");
		for (Mount m : config.mounts) {
			for (SyncRule sr : m.once) {
				if (sr.mode.equals("pull") || sr.mode.equals("sync")) {
					fileWalk(sr.path, sr.exclude);
				}
			}
		}
	}
	
	public void fileWalk(String path, List<String> exclude) {
		if (exclude.contains(path)) {
			return;
		}
		File f = pathResolver.localPath(path);
		String fingerprint = PathResolver.fingerprint(f);
		toServer.pathFingerprint(path, fingerprint);
		if (f.isDirectory()) {
			File[] files = f.listFiles();
			for (File child : files) {
				fileWalk(path + "/" + child.getName(), exclude);
			}
		}
	}
}

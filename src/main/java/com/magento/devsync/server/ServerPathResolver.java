package com.magento.devsync.server;

import java.io.File;
import java.util.List;

import com.magento.devsync.communications.PathResolver;
import com.magento.devsync.config.Mount;
import com.magento.devsync.config.YamlFile;

public class ServerPathResolver extends PathResolver {
	
	private YamlFile config;

	public ServerPathResolver(YamlFile config) {
		this.config = config;
	}
	
	/**
	 * Convert a client path into a server side path, using the list
	 * of mount points.
	 */
	@Override
	public File clientPathToFile(String abstractPath) {
		// Go through the mount list to see if any of the prefixes match,
		// and if so replace the local path prefix with the remote path prefix.
		Mount lastAttempt = null;
		for (Mount m : config.mounts) {
			if (m.local.length() == 0 || m.local.equals(".")) {
				lastAttempt = m;
			} else if (abstractPath.equals(m.local)) {
				return new File(m.remote);
			} else {
				String prefix = m.local + "/";
				if (abstractPath.startsWith(prefix)) {
					return new File(m.remote + "/" + abstractPath.substring(prefix.length()));
				}
			}
		}
		if (lastAttempt != null) {
			return new File(lastAttempt.remote + "/" + abstractPath);
		}
		throw new RuntimeException("Unable to convert " + abstractPath + " to server path.");
	}
}

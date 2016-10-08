package com.magento.devsync.server;

import java.io.File;
import java.util.List;

import com.magento.devsync.communications.PathResolver;
import com.magento.devsync.config.Mount;
import com.magento.devsync.config.YamlFile;

public class ServerPathResolver extends PathResolver {
	
	private final List<Mount> mounts;

	public ServerPathResolver(YamlFile config) {
		this.mounts = config.mounts;
	}
	
	/**
	 * Convert a client path into a server side path, using the list
	 * of mount points.
	 */
	@Override
	public File localPath(String abstractPath) {
		// TODO Auto-generated method stub
		return new File(abstractPath);
	}

	/**
	 * Convert a server path into a client path, using list of mount points.
	 */
	@Override
	public String abstractPath(File localPath) {
		// TODO Auto-generated method stub
		return null;
	}

}

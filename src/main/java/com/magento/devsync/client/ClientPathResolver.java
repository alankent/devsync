package com.magento.devsync.client;

import java.io.File;

import com.magento.devsync.communications.PathResolver;

public class ClientPathResolver extends PathResolver {

	@Override
	public File localPath(String abstractPath) {
		// TODO Auto-generated method stub
		return new File(abstractPath); // TODO: THIS IS WRONG
	}

	@Override
	public String abstractPath(File localPath) {
		// TODO Auto-generated method stub
		return localPath.getPath();// TODO: THIS IS WRONG
	}

}

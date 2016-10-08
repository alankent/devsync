package com.magento.devsync.server;

import java.util.List;

import com.magento.devsync.communications.InclusionExclusionPath;
import com.magento.devsync.communications.SendMessage;
import com.magento.devsync.config.Mount;

public class Server implements Runnable {

	private SendMessage conn;
	private List<Mount> mounts;
	
	public Server(SendMessage conn) {
		this.conn = conn;
	}

	public void setMountPoints(List<Mount> mounts) {
		this.mounts = mounts;
	}

	public void initialSync(List<InclusionExclusionPath> paths) {
		// TODO Auto-generated method stub
		System.err.println("In INITIAL-SYNC");
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		
	}

}

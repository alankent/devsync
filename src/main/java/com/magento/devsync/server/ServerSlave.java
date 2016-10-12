package com.magento.devsync.server;

import com.magento.devsync.communications.Channel;
import com.magento.devsync.communications.Logger;
import com.magento.devsync.communications.Reactor;
import com.magento.devsync.filewatcher.ModifiedFileHistory;

public class ServerSlave implements Runnable {
	
	private Reactor slave;
		
	public ServerSlave(Channel channel, ServerMaster serverMaster, Logger logger, ModifiedFileHistory modifiedFileHistory) {
		slave = new Reactor(channel, serverMaster, logger, modifiedFileHistory);
	}

	@Override
	public void run() {
		slave.run();
	}
}

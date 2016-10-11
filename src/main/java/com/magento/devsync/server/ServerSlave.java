package com.magento.devsync.server;

import com.magento.devsync.communications.Channel;
import com.magento.devsync.communications.Logger;
import com.magento.devsync.communications.Reactor;

public class ServerSlave implements Runnable {
	
	private Reactor slave;
		
	public ServerSlave(Channel channel, ServerMaster serverMaster, Logger logger) {
		slave = new Reactor(channel, serverMaster, logger);
	}

	@Override
	public void run() {
		slave.run();
	}
}

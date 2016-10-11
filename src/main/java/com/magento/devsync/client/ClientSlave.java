package com.magento.devsync.client;

import com.magento.devsync.communications.Channel;
import com.magento.devsync.communications.Logger;
import com.magento.devsync.communications.Reactor;
import com.magento.devsync.config.YamlFile;

public class ClientSlave implements Runnable {
	
	private Reactor slave;
		
	public ClientSlave(Channel channel, YamlFile config, Logger logger, ClientMaster clientMaster) {
		slave = new Reactor(channel, clientMaster, config, logger);
	}

	@Override
	public void run() {
		slave.run();
	}
}

package com.magento.devsync.server;

import java.io.IOException;
import java.net.Socket;

import com.magento.devsync.communications.Channel;
import com.magento.devsync.communications.ChannelMultiplexer;
import com.magento.devsync.communications.Logger;
import com.magento.devsync.filewatcher.ModifiedFileHistory;

/**
 * Each new client socket connection request creates a new server instance.
 * When the client exits, the configuration details are discarded, allowing
 * the configuration file to change and a new connection established.
 * Thus the server needs to be more careful about cleaning up resources than
 * the client.
 */
public class ServerConnection implements Runnable {

	private ServerMaster master;
	private ServerSlave slave;
	
	public ServerConnection(Socket socket, Logger logger) throws IOException {
		logger.log("SERVER CREATED");
		
		ChannelMultiplexer multiplexer = new ChannelMultiplexer(socket, logger);
		Channel toClientChannel = new Channel(1, multiplexer);
		Channel fromClientChannel = new Channel(0, multiplexer);
		new Thread(multiplexer, "Server-Multiplexer").start();
		
		ModifiedFileHistory history = new ModifiedFileHistory();
		
		master = new ServerMaster(toClientChannel, logger, history);
		
		// Slave is a child thread, master is the current thread.
		logger.log("Spawning slave thread");
		slave = new ServerSlave(fromClientChannel, master, logger, history);
		new Thread(slave, "Server-Slave").start();
	}

	@Override
	public void run() {
		master.run();
	}
}

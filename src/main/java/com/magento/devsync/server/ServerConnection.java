package com.magento.devsync.server;

import java.io.IOException;
import java.net.Socket;

import com.magento.devsync.communications.Channel;
import com.magento.devsync.communications.ChannelMultiplexer;
import com.magento.devsync.communications.ConnectionLost;
import com.magento.devsync.communications.Logger;
import com.magento.devsync.communications.Reactor;
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
    private Reactor slave;

    public ServerConnection(Socket socket, Logger logger, String templateDir) throws IOException {
        logger.debug("SERVER CREATED");

        ChannelMultiplexer multiplexer = new ChannelMultiplexer(socket);
        Channel toClientChannel = new Channel(1, multiplexer);
        Channel fromClientChannel = new Channel(0, multiplexer);
        Thread multiThread = new Thread(multiplexer, "Server-Multiplexer");
        multiThread.setDaemon(true);
        multiThread.start();

        ModifiedFileHistory history = new ModifiedFileHistory();

        master = new ServerMaster(toClientChannel, logger, history, templateDir);

        // Slave is a child thread, master is the current thread.
        logger.debug("Spawning slave thread");
        slave = new Reactor(fromClientChannel, master, logger, history);
        Thread slaveThread = new Thread(slave, "Server-Slave");
        slaveThread.setDaemon(true);
        slaveThread.start();
    }

    @Override
    public void run() {
        try {
            master.run();
        } catch (ConnectionLost e) {
            // Lost connection, exit.
        }
    }
}

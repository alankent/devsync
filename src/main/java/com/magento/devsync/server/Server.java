package com.magento.devsync.server;

import java.io.IOException;
import java.net.Socket;

import com.magento.devsync.communications.ProtocolSpec;
import com.magento.devsync.communications.ReceiveMessage;
import com.magento.devsync.communications.SendMessage;
import com.magento.devsync.communications.SyncTreeWalker;
import com.magento.devsync.config.YamlFile;

public class Server implements Runnable {

	private Socket socket;
	private SendMessage sender;
	private ReceiveMessage receiver;
	private YamlFile config;
	private ServerPathResolver pathResolver;
	
	/**
	 * Spawn threads to listen for requests and for sending responses.
	 * @param socket The Socket to use for communication.
	 * @return The Server instance.
	 */
	public static Server start(Socket socket) {
		try {
			Server s = new Server(socket);
			
			// Spawn a new thread per socket connection.
			Thread t = new Thread(s);
			t.start();
			return s;
			
		} catch (IOException e) {
			throw new RuntimeException("Failed to establish listening socket: " + e.getMessage());
		}
		
	}
	
	private Server(Socket socket) throws IOException {
		System.out.println("SERVER CREATED");
		this.socket = socket;
		
		sender = new SendMessage(socket.getOutputStream());
				
		receiver = new ReceiveMessage(socket.getInputStream(), sender, this);
	}

	public void setMountPoints(YamlFile config) {
		System.out.println("IN SERVER SET-MOUNT-POINTS");
		if (config == null) System.out.println("Config is NUL in SET-MOUNT");
		if (config != null) System.out.println("Config is NUL in SET-MOUNT");
		this.config = config;
		this.pathResolver = new ServerPathResolver(config);
		receiver.setPathResovler(this.pathResolver, config);
	}

	/**
	 * This is called by the receiver thread.
	 * To avoid deadlocks, spawn a separate thread so caller can go back
	 * waiting for the next request in parallel.
	 * @param paths
	 */
	public void initialSync() {
		System.err.println("In INITIAL-SYNC");
		if (config == null) System.out.println("config is null (1)");
		if (config != null) System.out.println("config is NOT null (1)");
		
		new Thread(new Runnable() {
			public void run() {
				
				if (config == null) System.out.println("config is null (2)");
				if (config != null) System.out.println("config is NOT null (2)");
				SyncTreeWalker walker = new SyncTreeWalker(sender, config, pathResolver);
				walker.serverToClientWalk();
				
				sender.syncComplete();
			}
		}).start();
	}

	@Override
	public void run() {
		receiver.run();
	}

}

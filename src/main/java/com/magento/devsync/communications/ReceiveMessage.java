package com.magento.devsync.communications;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import com.magento.devsync.client.Client;
import com.magento.devsync.client.ClientPathResolver;
import com.magento.devsync.config.Mount;
import com.magento.devsync.config.YamlFile;
import com.magento.devsync.server.Server;
import com.magento.devsync.server.ServerPathResolver;

/**
 * Background thread for receiving messages from the other endpoint.
 * Shared by both client and server as much of protocol is in common.
 */
public class ReceiveMessage implements Runnable {
	
	private InputStream sock;
	private SendMessage sender;
	private Server server;
	private Client client;
	private PathResolver pathResolver;
	private boolean gracefulExit = false;
	private YamlFile config;
	
	public ReceiveMessage(InputStream sock, SendMessage sender, Client client, YamlFile config) {
		this.sock = sock;
		this.sender = sender;
		this.client = client;
		this.server = null;
		this.config = config;
		this.pathResolver = new ClientPathResolver();
	}
	
	public ReceiveMessage(InputStream sock, SendMessage sender, Server server) {
		this.sock = sock;
		this.sender = sender;
		this.client = null;
		this.server = server;
	}
	
	/**
	 * The path resolver for the server is not known until a message arrives
	 * with the required configuration to do resolution.
	 * So this cannot be done in constructor.
	 * @param pathResolver
	 */
	public void setPathResovler(PathResolver pathResolver, YamlFile config) {
		this.config = config;
		this.pathResolver = pathResolver;
	}

	/**
	 * Keep receiving messages until the socket is closed.
	 */
	public void run() {
		try {
			while (true) {
				processMessage();
			}
		} catch (Exception e) {
			if (gracefulExit) {
				System.out.println(e.getMessage());
			} else {
				e.printStackTrace();
			}
		}
	}
	
	private void processMessage() {
		
		try {
		
			// Read message length.
			ByteBuffer bb = ByteBuffer.allocate(Integer.BYTES);
			readBytes(bb, Integer.BYTES);
			bb.flip();
			int payloadLength = bb.getInt();

			ByteBuffer payload = ByteBuffer.allocate(payloadLength);
			readBytes(payload, payloadLength);
			payload.flip();
			
			int command = payload.get();
			
			switch (command) {
			
				case ProtocolSpec.CHECK_PROTOCOL_VERSION: {
					log("RECV: Check protocol");
					int version = payload.getInt();
					if (version != ProtocolSpec.PROTOCOL_VERSION) {
						sender.errorMessage("Client and server are at different protocol versions. Aborting");
						System.err.println("Protocol version wrong, crash and burn.");
						System.exit(1);
					}
					break;
				}
				
				case ProtocolSpec.SET_MOUNT_POINTS: {
					log("RECV: set config");
					String yamlFile = getString(payload);
					config = YamlFile.parseYaml(yamlFile);
					if (server != null) {
						server.setMountPoints(config);
					} else {
						throw new RuntimeException("SET-MOUNT-POINTS should never be sent to client");
					}
					break;
				}
				
				case ProtocolSpec.ERROR_MESSAGE: {
					log("RECV: error");
					String message = getString(payload);
					System.err.print(message);
					break;
				}

				case ProtocolSpec.INITIAL_SYNC: {
					log("RECV: initial sync request");
					if (server != null) {
						server.initialSync();
					} else {
						throw new RuntimeException("INITIAL-SYNC should never be sent to client");
					}
					break;
				}

				case ProtocolSpec.SYNC_COMPLETE: {
					log("RECV: initial sync COMPLETE");
					if (client != null) {
						client.syncComplete();
					} else {
						throw new RuntimeException("SYNC-COMPLETE should never be sent to server");
					}
					break;
				}

				case ProtocolSpec.WATCH_LIST: {
					log("RECV: watch file changes");
					if (server != null) {
						//TODO: START WATCHING FILES: server.watchList();
					} else {
						throw new RuntimeException("WATCH-LIST should never be sent to client");
					}
					break;
				}

				case ProtocolSpec.PATH_FINGERPRINT: { 
					log("RECV: fingerprint");
					String path = getString(bb);
					String fingerprint = getString(bb);
					pathFingerprint(path, fingerprint);
					break;
				}

				case ProtocolSpec.PATH_DELETED: {
					log("RECV: delete path");
					String path = getString(bb);
					pathDeleted(path);
					break;
				}

				case ProtocolSpec.SEND_ME_FILE: {
					log("RECV: send me file");
					String path = getString(bb);
					File f = pathResolver.localPath(path);
					int mode = PathResolver.mode(f);
					sender.writeFile(path, mode, f);
					break;
				}

				case ProtocolSpec.WRITE_FILE: {
					log("RECV: write file");
					String path = getString(bb);
					int mode = bb.getInt();
					long fileLength = bb.getLong();
					//TODO: writeFile(path, mode, pathResolver.localPath(path));
					break;
				}

				case ProtocolSpec.CREATE_DIRECTORY: {
					log("RECV: create directory");
					String path = getString(bb);
					int mode = bb.getInt();
					createDirectory(path, mode);
					break;
				}

				default: {
					// TODO
					break;
				}
			}
			
		} catch (Exception e) {
			// TODO
		}
	}

	private void log(String message) {
		System.out.println(message);
	}
	
	private void pathDeleted(String path) {
		// TODO Auto-generated method stub
		
	}

	private void pathFingerprint(final String path, final String remoteFingerprint) {
		File localPath = pathResolver.localPath(path);
		if (localPath.exists()) {
			if (localPath.isDirectory()) {
				if (server != null) {
					new Thread(new Runnable() {
						public void run() {
							sender.errorMessage("Cannot sync " + path + " as one is file, other is directory");
						}
					}).start();
				} else {
					System.err.println("Cannot sync " + path + " as one is file, other is directory");
				}
			} else {
				String localFingerprint = PathResolver.fingerprint(localPath);
				if (localFingerprint != remoteFingerprint) {
					// The file is different - please send us a copy!
					new Thread(new Runnable() {
						public void run() {
							sender.sendMeFile(path);
						}
					}).start();
				}
			}
		} else {
			// I don't have the file, please send me a copy!
			new Thread(new Runnable() {
				public void run() {
					sender.sendMeFile(path);
				}
			}).start();
		}
	}
	
	private void createDirectory(String path, int mode) {
		File f = pathResolver.localPath(path);
		if (f.isDirectory()) {
			// Already exists as directory - only need to check file permissions
			// TODO:
		} else if (f.exists()) {
			// TODO: Umm, directory exists, but as a file! SKY IS FALLING!
		} else {
			if (!f.mkdir()) {
				// TODO: Umm, create dir failed!??
			}
			// TODO: Does not set file mode on directories.
		}
	}

	private static String getString(ByteBuffer bb) {
		int bufLen = bb.getInt();
		byte[] strBuf = new byte[bufLen];
		bb.get(strBuf);
		return new String(strBuf, Charset.forName("UTF-8"));
	}
	
	private static boolean getBoolean(ByteBuffer bb) {
		return bb.get() != 0;
	}
	
	/**
	 * Keep reading until the requested number of bytes has been read (or the socket closed).
	 * @param bb Buffer to read into.
	 * @param length The number of bytes to read, guaranteed.
	 */
	private void readBytes(ByteBuffer bb, int length) {
		try {
			int remaining = length;
			while (remaining > 0) {
				int actual = sock.read(bb.array(), bb.position(), remaining);
				if (actual < 0) {
					// Client closed connection on us.
					gracefulExit = true;
					throw new RuntimeException("Connection lost, shutting down");
				}
				remaining -= actual;
				bb.position(bb.position() + actual);
			}
		} catch (IOException e) {
			throw new RuntimeException("Failed to read from socket", e);
		}
	}
}

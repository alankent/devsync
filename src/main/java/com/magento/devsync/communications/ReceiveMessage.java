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
import com.magento.devsync.server.Server;
import com.magento.devsync.server.ServerPathResolver;

public class ReceiveMessage implements Runnable {
	
	private InputStream sock;
	private ProtocolSpec sender;
	private Server server;
	private Client client;
	private PathResolver pathResolver;
	private boolean gracefulExit = false;
	
	public ReceiveMessage(InputStream sock, ProtocolSpec sender, Client client) {
		this.sock = sock;
		this.sender = sender;
		this.client = client;
		this.server = null;
		this.pathResolver = new ClientPathResolver();
	}
	
	public ReceiveMessage(InputStream sock, ProtocolSpec sender, Server server) {
		this.sock = sock;
		this.sender = sender;
		this.client = null;
		this.server = server;
		this.pathResolver = new ServerPathResolver();
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
					int version = payload.getInt();
					if (version != ProtocolSpec.PROTOCOL_VERSION) {
						sender.errorMessage("Client and server are at different protocol versions. Aborting");
						throw new RuntimeException("Protocol version wrong, crash and burn.");
					}
					break;
				}
				
				case ProtocolSpec.SET_MOUNT_POINTS: {
					List<Mount> mounts = new ArrayList<Mount>();
					int numMounts = payload.getInt();
					for (int i = 0; i < numMounts; i++) {
						Mount m = new Mount();
						m.local = getString(payload);
						m.remote = getString(payload);
						mounts.add(m);
					}
					if (server != null) {
						server.setMountPoints(mounts);
					} else {
						throw new RuntimeException("SET-MOUNT-POINTS should never be sent to client");
					}
					break;
				}
				
				case ProtocolSpec.ERROR_MESSAGE: {
					String message = getString(payload);
					System.err.print(message);
					break;
				}

				case ProtocolSpec.INITIAL_SYNC: {
					int numRules = payload.getInt();
					List<InclusionExclusionPath> paths = new ArrayList<InclusionExclusionPath>();
					for (int i = 0; i < numRules; i++) {
						InclusionExclusionPath p = new InclusionExclusionPath();
						p.exclude = getBoolean(payload);
						p.pathPattern = getString(payload);
						paths.add(p);
					}
					if (server != null) {
						server.initialSync(paths);
					} else {
						throw new RuntimeException("INITIAL-SYNC should never be sent to client");
					}
					break;
				}

				case ProtocolSpec.SYNC_COMPLETE: {
					if (client != null) {
						client.syncComplete();
					} else {
						throw new RuntimeException("SYNC-COMPLETE should never be sent to server");
					}
					break;
				}

				case ProtocolSpec.WATCH_LIST: {
					Object configuration = null; // TODO
					if (server != null) {
						server.watchList(configuration);
					} else {
						throw new RuntimeException("WATCH-LIST should never be sent to client");
					}
					break;
				}

				case ProtocolSpec.PATH_FINGERPRINT: { 
					String path = getString(bb);
					String fingerprint = getString(bb);
					pathFingerprint(path, fingerprint);
					break;
				}

				case ProtocolSpec.PATH_DELETED: {
					String path = getString(bb);
					pathDeleted(path);
					break;
				}

				case ProtocolSpec.SEND_ME_FILE: {
					String path = getString(bb);
					File f = pathResolver.localPath(path);
					int mode = PathResolver.mode(f);
					sender.writeFile(path, mode, f);
					break;
				}

				case ProtocolSpec.WRITE_FILE: {
					String path = getString(bb);
					int mode = bb.getInt();
					long fileLength = bb.getLong();
					writeFile(path, mode, pathResolver.localPath(path));
					break;
				}

				case ProtocolSpec.CREATE_DIRECTORY: {
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
	
	private void pathDeleted(String path) {
		// TODO Auto-generated method stub
		
	}

	private void pathFingerprint(String path, String remoteFingerprint) {
		File localPath = pathResolver.localPath(path);
		if (localPath.exists()) {
			if (localPath.isDirectory()) {
				if (server != null) {
					sender.errorMessage("Cannot sync " + path + " as one is file, other is directory");
				} else {
					System.err.println("Cannot sync " + path + " as one is file, other is directory");
				}
			} else {
				String localFingerprint = PathResolver.fingerprint(localPath);
				if (localFingerprint != remoteFingerprint) {
					// The file is different - please send us a copy!
					sender.sendMeFile(path);
				}
			}
		} else {
			// I don't have the file, please send me a copy!
			sender.sendMeFile(path);
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

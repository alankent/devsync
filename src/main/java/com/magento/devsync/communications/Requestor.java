package com.magento.devsync.communications;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import com.magento.devsync.config.YamlFile;

/**
 * Serializes a request and sends it over the network connection to the other endpoint.
 * Used for sending both client->server and server->client requests.
 */
public class Requestor {
	
	private Logger logger;
	private Channel channel;

	public Requestor(Channel channel, Logger logger) {
		this.channel = channel;
		this.logger = logger;
	}
	
	public boolean checkProtocolVersion(int version) throws IOException {
		log("SEND: Check protocol version");
		MessageWriter msg = new MessageWriter();
		msg.putByte(ProtocolSpec.CHECK_PROTOCOL_VERSION);
		msg.putInt(version);
		channel.send(msg);
		return receiveOkOrNotOk();
	}
	
	public boolean setConfig(YamlFile config) throws IOException {
		log("SEND: Send configuration file to server");
		MessageWriter msg = new MessageWriter();
		msg.putByte(ProtocolSpec.SET_CONFIG);
		msg.putString(config.contents);
		channel.send(msg);
		return receiveOkOrNotOk();
	}
	
	public boolean errorMessage(String message) throws IOException {
		log("SEND: Error: " + message);
		MessageWriter msg = new MessageWriter();
		msg.putByte(ProtocolSpec.ERROR_MESSAGE);
		msg.putString(message);
		channel.send(msg);
		return receiveOkOrNotOk();
	}
	
	public boolean initialSync() throws IOException {
		log("SEND: Trigger server initial sync");
		MessageWriter msg = new MessageWriter();
		msg.putByte(ProtocolSpec.START_SERVER_SYNC);
		channel.send(msg);
		return receiveOkOrNotOk();
	}

	public boolean syncComplete() throws IOException {
		log("SEND: Server sync complete");
		MessageWriter msg = new MessageWriter();
		msg.putByte(ProtocolSpec.SERVER_SYNC_COMPLETE);
		channel.send(msg);
		return receiveOkOrNotOk();
	}

	public boolean pathFingerprint(String path, boolean canExecute, File contents, String fingerprint) throws IOException {
		log("SEND: Fingerprint: " + path + " " + fingerprint);
		MessageWriter msg = new MessageWriter();
		msg.putByte(ProtocolSpec.PATH_FINGERPRINT);
		msg.putString(path);
		msg.putString(fingerprint);
		channel.send(msg);
		
		MessageReader resp = channel.receive();
		switch (resp.getByte()) {
			case ProtocolSpec.SEND_ME_FILE: {
				log("RECV: SEND_ME_FILE: Write file to disk: " + path + " " + (canExecute ? "exe" : "plain"));
				return writeFile(path, canExecute, contents);
			}
			case ProtocolSpec.OK: {
				log("RECV: OK");
				return true;
			}
			case ProtocolSpec.NOT_OK: {
				log("RECV: NOT-OK");
				return false;
			}
			default: {
				log("RECV: <unexpected-response>");
				throw new RuntimeException("Protocol error");
			}
		}
//		return false;
	}

	public boolean pathDeleted(String path) throws IOException {
		log("SEND: Delete: " + path);
		MessageWriter msg = new MessageWriter();
		msg.putByte(ProtocolSpec.PATH_DELETED);
		msg.putString(path);
		channel.send(msg);
		return receiveOkOrNotOk();
	}

	/**
	 * This needs to be synchronized as we need to send message and file contents
	 * together, with no other messages sneaking in the middle.
	 */
	public boolean writeFile(String path, boolean canExecute, File contents) {
		
		log("SEND: WRITE_FILE: Write file to disk: " + path + " " + (canExecute ? "exe" : "plain"));
		try (FileInputStream in = new FileInputStream(contents)) {
			
			MessageWriter msg = new MessageWriter();
			msg.putByte(ProtocolSpec.WRITE_FILE);
			msg.putString(path);
			msg.putBoolean(canExecute);
			
			byte[] buf = new byte[1024 * 1024];
			while (true) {
				int bytesRead = readFill(in, buf);
				boolean eof = bytesRead < buf.length;
				logger.log("  write file eof=" + Boolean.toString(eof) + " bytes=" + bytesRead);
				msg.putBoolean(eof);
				msg.putInt(bytesRead);
				msg.putBytes(buf,  0,  bytesRead);
				channel.send(msg);
				if (!receiveOkOrNotOk()) {
					return false;
				}
				if (eof) {
					break;
				}
				log("SEND: MORE-DATA");
				msg = new MessageWriter();
				msg.putByte(ProtocolSpec.MORE_DATA);
			}
		} catch (Exception e) {
			log("Problem reading " + contents);
			return false;
		}
		return true;
	}
	
	private static int readFill(FileInputStream input, byte[] buf) throws IOException {
		int offset = 0;
		int remaining = buf.length;
		while (remaining > 0) {
			int bytesRead = input.read(buf, offset, remaining);
			if (bytesRead <= 0) {
				return offset;
			}
			offset += bytesRead;
			remaining -= bytesRead;
		}
		return offset;
	}

	public boolean createDirectory(String path) throws IOException {
		log("SEND: Create directory: " + path);
		MessageWriter msg = new MessageWriter();
		msg.putByte(ProtocolSpec.CREATE_DIRECTORY);
		msg.putString(path);
		channel.send(msg);
		return receiveOkOrNotOk();
	}
	
	private boolean receiveOkOrNotOk() {
		MessageReader resp = channel.receive();
		int cmd = resp.getByte();
		if (cmd == ProtocolSpec.OK) {
			log("RECV: OK");
			return true;
		}
		if (cmd == ProtocolSpec.NOT_OK) {
			String errorMessage = resp.getString();
			log("RECV: NOT-OK: " + errorMessage);
			System.err.println(errorMessage);
			return false;
		}
		throw new RuntimeException("Protocol error!");
	}
	
	private void log(String message) {
		logger.log(message);
	}
}

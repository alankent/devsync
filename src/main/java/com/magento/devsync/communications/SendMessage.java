package com.magento.devsync.communications;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.List;

import com.magento.devsync.config.Mount;
import com.magento.devsync.config.YamlFile;

/**
 * Serializes a request and sends it over the network connection to the other endpoint.
 * Used for sending both client->server and server->client messages.
 */
public class SendMessage implements ProtocolSpec {
	
	private OutputStream os;

	public SendMessage(OutputStream os) {
		this.os = os;
	}
	
	public void checkProtocolVersion(int version) {
		log("SEND: Check protocol version");
		ByteArrayOutputStream bb = new ByteArrayOutputStream();
		bb.write(ProtocolSpec.CHECK_PROTOCOL_VERSION);
		writeInteger(bb, version);
		sendMessage(bb);
	}
	
	public void setMountPoints(YamlFile config) {
		log("SEND: Send configuration file to server");
		ByteArrayOutputStream bb = new ByteArrayOutputStream();
		bb.write(ProtocolSpec.SET_MOUNT_POINTS);
		writeString(bb, config.contents);
		sendMessage(bb);
	}
	
	public void errorMessage(String message) {
		log("SEND: Error: " + message);
		ByteArrayOutputStream bb = new ByteArrayOutputStream();
		bb.write(ProtocolSpec.ERROR_MESSAGE);
		writeString(bb, message);
		sendMessage(bb);
	}
	
	public void initialSync() {
		log("SEND: Trigger server initial sync");
		ByteArrayOutputStream bb = new ByteArrayOutputStream();
		bb.write(ProtocolSpec.INITIAL_SYNC);
		sendMessage(bb);
	}

	public void syncComplete() {
		log("SEND: Server sync complete");
		ByteArrayOutputStream bb = new ByteArrayOutputStream();
		bb.write(ProtocolSpec.SYNC_COMPLETE);
		sendMessage(bb);
	}

	public void watchList() {
		log("SEND: Server, start watching file system!");
		ByteArrayOutputStream bb = new ByteArrayOutputStream();
		bb.write(ProtocolSpec.WATCH_LIST);
		sendMessage(bb);
	}

	public void pathFingerprint(String path, String fingerprint) {
		log("SEND: Fingerprint: " + path + " " + fingerprint);
		ByteArrayOutputStream bb = new ByteArrayOutputStream();
		bb.write(ProtocolSpec.PATH_FINGERPRINT);
		writeString(bb, path);
		writeString(bb, fingerprint);
		sendMessage(bb);
	}

	public void pathDeleted(String path) {
		log("SEND: Delete: " + path);
		ByteArrayOutputStream bb = new ByteArrayOutputStream();
		bb.write(ProtocolSpec.PATH_DELETED);
		writeString(bb, path);
		sendMessage(bb);
	}

	public void sendMeFile(String path) {
		log("SEND: Send me file: " + path);
		ByteArrayOutputStream bb = new ByteArrayOutputStream();
		bb.write(ProtocolSpec.SEND_ME_FILE);
		writeString(bb, path);
		sendMessage(bb);
	}

	public void writeFile(String path, int mode, File contents) {
		log("SEND: Write file to disk: " + path + " " + mode);
		ByteArrayOutputStream bb = new ByteArrayOutputStream();
		bb.write(ProtocolSpec.WRITE_FILE);
		writeString(bb, path);
		writeInteger(bb, mode);
		writeLong(bb, contents.length());
		sendMessage(bb);
	}

	public void createDirectory(String path, int mode) {
		log("SEND: Create directory: " + path + " " + mode);
		ByteArrayOutputStream bb = new ByteArrayOutputStream();
		bb.write(ProtocolSpec.CREATE_DIRECTORY);
		writeString(bb, path);
		writeInteger(bb, mode);
		sendMessage(bb);
	}
	
	private static void writeInteger(ByteArrayOutputStream bb, int num) {
		ByteBuffer lenBuf = ByteBuffer.allocate(Integer.BYTES);
		lenBuf.putInt(num);
		bb.write(lenBuf.array(), 0, lenBuf.position());
	}
	
	private static void writeLong(ByteArrayOutputStream bb, long num) {
		ByteBuffer lenBuf = ByteBuffer.allocate(Long.BYTES);
		lenBuf.putLong(num);
		bb.write(lenBuf.array(), 0, lenBuf.position());
	}
	
	private static void writeString(ByteArrayOutputStream bb, String string) {
		byte[] buf = string.getBytes(Charset.forName("UTF-8"));
		writeInteger(bb, buf.length);
		bb.write(buf, 0, buf.length);
	}

	private static void writeBoolean(ByteArrayOutputStream bb, boolean val) {
		bb.write(val ? 1 : 0);
	}

	synchronized private void sendMessage(ByteArrayOutputStream bb) {
		try {
			byte[] payload = bb.toByteArray();
			ByteBuffer lenBuf = ByteBuffer.allocate(Integer.BYTES);
			lenBuf.putInt(payload.length);
			os.write(lenBuf.array(), 0, lenBuf.position());
			os.write(payload, 0, payload.length);
		} catch (Exception e) {
			throw new RuntimeException("Error writing to socket", e);
		}
	}
	
	private void log(String message) {
		System.out.println(message);
	}
}

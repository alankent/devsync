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
		ByteArrayOutputStream bb = new ByteArrayOutputStream();
		bb.write(ProtocolSpec.CHECK_PROTOCOL_VERSION);
		writeInteger(bb, version);
		sendMessage(bb);
	}
	
	public void setMountPoints(List<Mount> mounts) {
		ByteArrayOutputStream bb = new ByteArrayOutputStream();
		bb.write(ProtocolSpec.SET_MOUNT_POINTS);
		writeInteger(bb, mounts.size());
		for (Mount m: mounts) {
			writeString(bb, m.local);
			writeString(bb, m.remote);
		}
		sendMessage(bb);
	}
	
	public void errorMessage(String message) {
		ByteArrayOutputStream bb = new ByteArrayOutputStream();
		bb.write(ProtocolSpec.ERROR_MESSAGE);
		writeString(bb, message);
		sendMessage(bb);
	}
	
	public void initialSync(List<InclusionExclusionPath> pathList) {
		ByteArrayOutputStream bb = new ByteArrayOutputStream();
		bb.write(ProtocolSpec.INITIAL_SYNC);
		writeInteger(bb, pathList.size());
		for (InclusionExclusionPath p : pathList) {
			writeBoolean(bb, p.exclude);
			writeString(bb, p.pathPattern);
		}
		sendMessage(bb);
	}

	public void syncComplete() {
		ByteArrayOutputStream bb = new ByteArrayOutputStream();
		bb.write(ProtocolSpec.SYNC_COMPLETE);
		sendMessage(bb);
	}

	public void watchList(Object configuration) {
		ByteArrayOutputStream bb = new ByteArrayOutputStream();
		bb.write(ProtocolSpec.WATCH_LIST);
		// TODO: Config
		sendMessage(bb);
	}

	public void pathFingerprint(String path, String fingerprint) {
		ByteArrayOutputStream bb = new ByteArrayOutputStream();
		bb.write(ProtocolSpec.PATH_FINGERPRINT);
		writeString(bb, path);
		writeString(bb, fingerprint);
		sendMessage(bb);
	}

	public void pathDeleted(String path) {
		ByteArrayOutputStream bb = new ByteArrayOutputStream();
		bb.write(ProtocolSpec.PATH_DELETED);
		writeString(bb, path);
		sendMessage(bb);
	}

	public void sendMeFile(String path) {
		ByteArrayOutputStream bb = new ByteArrayOutputStream();
		bb.write(ProtocolSpec.SEND_ME_FILE);
		writeString(bb, path);
		sendMessage(bb);
	}

	public void writeFile(String path, int mode, File contents) {
		ByteArrayOutputStream bb = new ByteArrayOutputStream();
		bb.write(ProtocolSpec.WRITE_FILE);
		writeString(bb, path);
		writeInteger(bb, mode);
		writeLong(bb, contents.length());
		sendMessage(bb);
	}

	public void createDirectory(String path, int mode) {
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
}

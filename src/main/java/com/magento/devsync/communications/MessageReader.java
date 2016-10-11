package com.magento.devsync.communications;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class MessageReader {

	private ByteBuffer payload;
	
	protected MessageReader(ByteBuffer payload) {
		this.payload = payload;
	}

	public String getString() {
		int bufLen = payload.getInt();
		byte[] strBuf = new byte[bufLen];
		payload.get(strBuf);
		return new String(strBuf, StandardCharsets.UTF_8);
	}
	
	public boolean getBoolean() {
		return payload.get() != 0;
	}
	
	public byte getByte() {
		return payload.get();
	}
	
	public ByteBuffer getBytes(int length) {
		if (length > payload.remaining()) {
			length = payload.remaining();
		}
		ByteBuffer buf = payload.slice();
		buf.limit(length);
		payload.position(payload.position() + length);
		return buf;
	}

	public int getInt() {
		return payload.getInt();
	}
	
	public long getLong() {
		return payload.getLong();
	}
}

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

    public void throwIfMore() {
        if (payload.remaining() > 0) {
            throw new RuntimeException("Undecoded content remaining in message buffer.");
        }
    }

    public void dump() {
        System.out.println("Message dump: position=" + payload.position());
        for (int i = 0; i < payload.limit(); i++) {
            System.out.print(" " + String.format("%02X", payload.get(i) & 0xff));
        }
        System.out.println();
    }
}

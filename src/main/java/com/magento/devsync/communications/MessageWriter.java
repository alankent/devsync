package com.magento.devsync.communications;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class MessageWriter {

    private ByteArrayOutputStream payload = new ByteArrayOutputStream();

    public void putByte(byte b) {
        payload.write(b);
    }

    public void putBytes(byte[] bytes, int offset, int length) {
        payload.write(bytes, offset, length);
    }

    public void putInt(int num) {
        ByteBuffer lenBuf = ByteBuffer.allocate(Integer.BYTES);
        lenBuf.putInt(num);
        payload.write(lenBuf.array(), 0, lenBuf.position());
    }

    public void putLong(long num) {
        ByteBuffer lenBuf = ByteBuffer.allocate(Long.BYTES);
        lenBuf.putLong(num);
        payload.write(lenBuf.array(), 0, lenBuf.position());
    }

    public void putString(String string) {
        byte[] buf = string.getBytes(StandardCharsets.UTF_8);
        putInt(buf.length);
        payload.write(buf, 0, buf.length);
    }

    public void putBoolean(boolean val) {
        payload.write(val ? 1 : 0);
    }

    protected byte[] toByteArray() {
        return payload.toByteArray();
    }
}

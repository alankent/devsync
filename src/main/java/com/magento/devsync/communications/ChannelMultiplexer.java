package com.magento.devsync.communications;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

/**
 * There is one socket, but the server or client process can spot a local file change
 * at any time and send it to the other end. This is achieved by having multiple
 * threads and multiplexing messages in different "channels" within the one socket.
 * Otherwise would need some kind of polling mechanism, which is less desirable.
 */
public class ChannelMultiplexer implements Runnable {

    private Socket socket;
    private InputStream input;
    private OutputStream output;
    private Logger logger;
    private Map<Integer,Channel> channels = new HashMap<>();
    private Object writeLock = new Object();
    private Object readLock = new Object();

    public ChannelMultiplexer(Socket socket, Logger logger) throws IOException {
        this.input = socket.getInputStream();
        this.output = socket.getOutputStream();
        this.logger = logger;
    }

    public void register(int channelNumber, Channel channel) {
        channels.put(channelNumber, channel);
    }

    /**
     * Send a message. Unlike receiving messages, the message to be sent is not
     * queued - it is done immediately.
     */
    public void send(int channelNumber, MessageWriter msg) throws IOException {
        byte[] payload = msg.toByteArray();
        ByteBuffer lenBuf = ByteBuffer.allocate(1 + Integer.BYTES);
        lenBuf.put((byte)channelNumber);
        lenBuf.putInt(payload.length);

        synchronized (writeLock) {
            output.write(lenBuf.array(), 0, lenBuf.position());
            output.write(payload, 0, payload.length);
            output.flush();
        }
    }

    /**
     * A separate thread is used to receive messages coming from the socket and
     * handing it off to the master or slave threads.
     */
    @Override
    public void run() {
        try {
            while (receive()) {
                // Loop until EOF.
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Put a 'null' in each channel message queue to indicate EOF.
        for (Channel channel : channels.values()) {
            channel.addReceivedMessage(null);
        }

        // Clean up resources.
        synchronized (writeLock) {
            try {
                output.close();
            } catch (IOException e) {
                // Ignore any problems closing the sockets.
            }
        }
        try {
            input.close();
        } catch (IOException e) {
            // Ignore any problems closing the sockets.
        }
        try {
            socket.close();
        } catch (IOException e) {
            // Ignore any problems closing the sockets.
        }
    }

    /**
     * Fetch one message from the socket, then look at its header to work out
     * which channel it is for.
     */
    synchronized private boolean receive() {

        // Read message length.
        synchronized (readLock) {
            ByteBuffer payloadHeader = ByteBuffer.allocate(1 + Integer.BYTES);
            if (!readBytes(payloadHeader, 1 + Integer.BYTES)) {
                return false;
            }
            payloadHeader.flip();
            int channelNumber = payloadHeader.get();
            int payloadLength = payloadHeader.getInt();

            // Read payload body, now we know the length.
            ByteBuffer payload = ByteBuffer.allocate(payloadLength);
            if (!readBytes(payload, payloadLength)) {
                return false;
            }
            payload.flip();

            //			logger.log("In multiplexer - got message for channel " + channelNumber);
            MessageReader msg = new MessageReader(payload);
            channels.get(channelNumber).addReceivedMessage(msg);
            return true;
        }
    }

    /**
     * Keep reading until the requested number of bytes has been read (or the socket closed).
     * @param bb Buffer to read into.
     * @param length The number of bytes to read, guaranteed.
     */
    private boolean readBytes(ByteBuffer bb, int length) {
        try {
            bb.clear();
            int remaining = length;
            while (remaining > 0) {
                int actual = input.read(bb.array(), bb.position(), remaining);
                if (actual < 0) {
                    // Client closed connection on us.
                    return false;
                }
                remaining -= actual;
                bb.position(bb.position() + actual);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read from socket", e);
        }
        return true;
    }
}

package com.magento.devsync.communications;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Channel {

    private int channelNumber;
    private ChannelMultiplexer multiplexer;
    private List<MessageReader> responses = new ArrayList<>();

    public Channel(int channelNumber, ChannelMultiplexer multiplexer) {
        this.channelNumber = (byte)channelNumber;
        this.multiplexer = multiplexer;
        multiplexer.register(channelNumber, this);
    }

    /**
     * You send messages from the thread you are on.
     * @throws ConnectionLost 
     */
    public void send(MessageWriter msg) throws IOException, ConnectionLost {
//        System.out.println("Channel:send(); " + channelNumber);
        multiplexer.send(channelNumber, msg);
    }

    /**
     * Responses
     * @return
     * @throws ConnectionLost 
     */
    synchronized public MessageReader receive() throws ConnectionLost {
        while (responses.isEmpty()) {
            try {
                wait();
            } catch (InterruptedException e) {
                // Ignore
            }
        }
        
        MessageReader resp = responses.get(0);
        if (resp == null) {
            throw new ConnectionLost(new RuntimeException("Found socket closed when tried to read."));
        }
        responses.remove(0);
        return resp;
    }

    synchronized protected void addReceivedMessage(MessageReader msg) {
        responses.add(msg);
        notifyAll();
    }
}

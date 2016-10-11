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
	 */
	public void send(MessageWriter msg) throws IOException {
//		System.out.println("Channel:send(); " + channelNumber);
		multiplexer.send(channelNumber, msg);
	}

	/**
	 * Responses
	 * @return
	 */
	synchronized public MessageReader receive() {
		while (responses.isEmpty()) {
			try {
				wait();
			} catch (InterruptedException e) {
				// Ignore
			}
		}
		return responses.remove(0);
	}
	
	synchronized protected void addReceivedMessage(MessageReader msg) {
		responses.add(msg);
		notifyAll();
	}
}

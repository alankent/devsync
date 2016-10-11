package com.magento.devsync.communications;

import java.io.File;
import java.io.IOException;
import java.util.List;

import com.magento.devsync.config.Mount;
import com.magento.devsync.config.YamlFile;

public interface ProtocolSpec {
	
	public static final int PROTOCOL_VERSION = 1;
	
	/**
	 * Immediately after socket is opened, client sends the protocol version to
	 * the server. The server responds with OK or NOT_OK.
	 */
	public static final byte CHECK_PROTOCOL_VERSION = 0;
	
	/**
	 * After the protocol version handshake completes, the client sends the
	 * config file to the server (this happens after protocol version handshake
	 * in case this step changes in future versions. The server responds with OK
	 * or NOT_OK.
	 */
	public static final byte SET_CONFIG = 1;

	/**
	 * At startup, the client first scans all its local paths that may need to
	 * be sent to the server sending a PATH-FINGERPRINT message per path. When
	 * done, a START-SERVER-SYNC message is sent to the server, causing the server to
	 * then check for any files it should be sending back. A response is
	 * always returned of OK or NOT-OK.
	 */
	public static final byte START_SERVER_SYNC = 2;
	
	/**
	 * When the server completes its SERVER-SYNC, it sends a SYNC-COMPLETE
	 * message back so the client knows the process is done. The client responds
	 * with OK or NOT-OK, after which the client and server both enter file
	 * watching mode. 
	 */
	public static final byte SERVER_SYNC_COMPLETE = 3;
	
	/**
	 * One endpoint is telling the other endpoint that a file or directory
	 * should exist with the specified fingerprint. If it already exists, that
	 * is good. If it does not exist (or is different), the response should be
	 * SEND-ME-FILE to request a copy of the file. This message is sent during
	 * the initial file synchronization process or later if a local file change
	 * was detected, but the file was recently received from the other endpoint.
	 * (For the latter, the file update message was probably due to the
	 * WRITE-FILE message. The response is a OK (the other end point has the
	 * file already), NO-OK (something went wrong), or SEND-ME-FILE (a copy is
	 * needed after looking at the fingerprint). A separate WRITE-FILE request
	 * is triggered next.
	 */
	public static final byte PATH_FINGERPRINT = 4;
	
	/**
	 * A response code to a fingerprint request, indicating the file is
	 * different so a copy should be sent.
	 */
	public static final byte SEND_ME_FILE = 5; 

	/**
	 * Initiate writing a file, with a response of OK or NOT-OK. This will be
	 * followed by a series of MORE-DATA messages (each with OK/NOT-OK) and
	 * finally a END-OF-DATA message (with OK/NOT-OK).
	 */
	public static final byte WRITE_FILE = 6;

	/**
	 * After a WRITE-FILE message indicating the start of a file transfer,
	 * a series of MORE-DATA requests are sent, containing file contents.
	 * Finally an END-OF-DATA request is sent. MORE-DATA has a response of
	 * OK or NOT_OK per message.
	 */
	public static final byte MORE_DATA = 7;

	/**
	 * See also MORE-DATA above. Finally an END-OF-DATA request is sent.
	 * END-OF-DATA has a response of OK or NOT_OK. In both cases the file
	 * should be closed off.
	 */
	public static final byte END_OF_DATA = 8;

	/**
	 * Tell other end to delete the specified file. OK or NOT-OK sent in response.
	 */
	public static final byte PATH_DELETED = 9;

	/**
	 * Tell other end to delete the specified file. OK or NOT-OK sent in
	 * response.
	 */
	public static final byte CREATE_DIRECTORY = 10;

	/**
	 * The server can send this message to the client in its channel to cause
	 * a message to be displayed on the screen.
	 */
	public static final byte ERROR_MESSAGE = 11;

	/**
	 * Response to a request indicating success.
	 */
	public static final byte OK = 12;
	
	/**
	 * Response to a request indicating something went wrong.
	 */
	public static final byte NOT_OK = 13;
}

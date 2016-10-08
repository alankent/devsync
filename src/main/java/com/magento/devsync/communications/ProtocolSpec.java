package com.magento.devsync.communications;

import java.io.File;
import java.util.List;

import com.magento.devsync.config.Mount;
import com.magento.devsync.config.YamlFile;

public interface ProtocolSpec {
	
	public static final int PROTOCOL_VERSION = 1;
	
	public static final byte CHECK_PROTOCOL_VERSION = 0;
	public static final byte ERROR_MESSAGE = 1;
	public static final byte INITIAL_SYNC = 2; 
	public static final byte SYNC_COMPLETE = 3; 
	public static final byte WATCH_LIST = 4;
	public static final byte PATH_FINGERPRINT = 5; 
	public static final byte PATH_DELETED = 6;
	public static final byte SEND_ME_FILE = 7; 
	public static final byte WRITE_FILE = 8;
	public static final byte CREATE_DIRECTORY = 9;
	public static final byte SET_MOUNT_POINTS = 10;
	
	
	/**
	 * Immediately after socket is opened, client sends the protocol version and
	 * config file to the server. The server can then close the socket if the
	 * version is not supported.
	 */
	public void checkProtocolVersion(int version);
	
	/**
	 * Tell the server the list of mount points to use. This is sent immediately
	 * after the protocol version is sent.
	 * 
	 * @param mounts
	 *            List of mount points, needed by server to convert client paths
	 *            into server path names.
	 */
	public void setMountPoints(YamlFile config);
	
	/**
	 * Send an error message from the server to the client for display on the screen.
	 */
	public void errorMessage(String message);

	/**
	 * At startup, the client first scans all its local paths that may need to
	 * be sent to the server sending a PATH-FINGERPRINT message per path. When
	 * done, a INITIAL-SYNC message is sent to the server, causing the server to
	 * then check for any files it should be sending back.
	 */
	public void initialSync();

	/**
	 * When the server completes its INITIAL-SYNC, it sends a SYNC-COMPLETE
	 * message back so the client knows the process is done. (The client will
	 * then send the WATCH-LIST message to start up the file-watching mode.)
	 */
	public void syncComplete();

	/**
	 * Sent from client to server at startup after initial synchronization has
	 * occurred to tell the server what files and directories to watch.
	 */
	public void watchList();

	/**
	 * One endpoint is telling the other endpoint that a file or directory
	 * should exist with the specified fingerprint. If it already exists, that
	 * is good. If it does not exist (or is different), the other endpoint
	 * should respond with a SEND-ME-FILE to request a copy of the file. This
	 * message is sent during the initial file synchronization process or later
	 * if a local file change was detected, but the file was recently received
	 * from the other endpoint. (For the latter, the file update message was
	 * probably due to the WRITE-FILE message
	 */
	public void pathFingerprint(String path, String fingerprint);

	/**
	 * Sent when a local file or directory has been deleted, for a file matching
	 * the watch list.
	 */
	public void pathDeleted(String path);

	/**
	 * A request for a specified file to be send via a WRITE-FILE request. The
	 * endpoint have have received a PATH-FINGERPRINT message and it has
	 * determined that it is worth getting a copy of the file.
	 */
	public void sendMeFile(String path);

	/**
	 * An endpoint is instructing the other end point to write a file to disk.
	 * This could be a result of a SEND-ME-FILE request or because a local file
	 * change was detected that the endpoint is sure needs to be replicated.
	 */
	public void writeFile(String path, int mode, File contents);

	/**
	 * An endpoint is instructing the other end point to create a new empty
	 * directory (if it does not already exist).
	 */
	public void createDirectory(String path, int mode);
}

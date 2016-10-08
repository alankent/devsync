Protocol
========

The protocol between the client and the server is an asynchronous,
unidirectional protocol. That is, the client and server can send a message
at any time, for any reason - the other end must always be ready. It also
means you cannot send a message and wait for the response you want - the
other end may send an unexpected message first which should be dealt with
(or at least not lost).

The reason for this is both ends will be watching the local file system for
changes, and sending an update whenever that occurs. This utility is also
only intended for a highly reliable communication channel, so there is not
so much acknowledgements going on. The other end will do its best, and if
it fails send an async message back.

It is likely there would be separate threads sending and receiving messages,
or else deadlock can occur with the write socket buffers of both endpoints
being full.

# Packet encoding

A binary wire protocol is used for speed of transfer of file contents.
A text representation exists for debugging purposes.

# Terminology

"File" is used to refer to a file on disk (excludes directories).
"Directory" is used to a directory (excludes files).
"Path" is used to refer to a filename or directory name.

"Endpoint" means the client or server process that is the current focus.
"Local" means something happening at the end-point.
"Remote" means something happening at the other end of the connection from the
end-point.

# Messages

INITIAL-SYNC: <configuration>
	At startup, the client first scans all its local paths that may need to be
	sent to the server sending a PATH-FINGERPRINT message per path. When done,
	a INITIAL-SYNC message is sent to the server, causing the server to then
	do a reciprical pass. 
	
SYNC-COMPLETE:
	When the server completes its INITIAL-SYNC, it sends a SYNC-COMPLETE
	message back so the client knows the process is done. (The client will then
	send the WATCH-LIST message to start up the file-watching mode.)

WATCH-LIST: <configuration>
	Only sent from client to server at startup, to tell it what files and
	directories to watch.
	
PATH-FINGERPRINT: <pathname> <fingerprint>
	One endpoint is telling the other endpoint that a file or directory should
	exist with the specified fingerprint. If it already exists, that is good.
	If it does not exist, the other endpoint should respond with a SEND-ME-FILE
	to request a copy of the file.  This message is sent during the initial
	file synchronization process or later if a local file change was detected,
	but the file was recently received from the other endpoint. (For the latter,
	the file update message was probably due to the WRITE-FILE message
	
PATH-DELETED: <filename>
	Sent when a local file or directory has been deleted, for a file matching
	the watch list.

SEND-ME-FILE: <filename>
	A request for a specified file. The endpoint have have received a
	PATH-FINGERPRINT message and it has determined that it is worth getting a
	copy of the file.
	
WRITE-FILE: <path> <mode> <binary-data>
	An endpoint is instructing the other end point to write a file to disk.
	This could be a result of a SEND-ME-FILE request or because a local file
	change was detected that the endpoint is sure needs to be replicated.
	
CREATE-DIRECTORY: <path> <mode>
	An endpoint is instructing the other end point to create a new empty
	directory (if it does not already exist).

# Common sequences of messages

Startup is as follows:

1. On startup, a directory tree walk is performed by the client with
   PATH-FINGERPRINT messages per file and directory.
2. The server may send SEND-ME-FILE requests to the client during this time
   that the client can perform during the file sync phase.
3. When the client has finished its pass, it sends a INITIAL-SYNC message to
   the server. This moves the client and server into the second stage.
4. The server walks the directory tree sending back PATH-FINGERPRINT messages
   to the client. If needed, the client will send SEND-ME-FILE requests back
   to the server. The server should respond if it can during the file sync
   process.
5. When the server completes its pass, it sends a SYNC-COMPLETE message to the
   client, causing transition to the final file watching state. 

In watching mode:

1. A list of "recently received files" is maintained, for a short period of
   time. Every time a file is received via WRITE-FILE, the path is added to
   the recently received files list.
2. If a local file change is detected, it is checked against the "recently
   recevied files" list.
   a. If we did just receive that file, the file change event was probably
      created from us writing the file to disk. But to be safe, send a
      PATH-FINGERPRINT message for the other end to check.
   b. If we did not recently receive that file, send a WRITE-FILE to the other
      endpoint.
3. If a local directory is created, or its file mode is changed, always send a
   CREATE-DIRECTORY message. The other end will only create the directory if
   it does not already exist, so there is no risk of a cycle here. 

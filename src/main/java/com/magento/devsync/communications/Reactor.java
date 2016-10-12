package com.magento.devsync.communications;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import com.magento.devsync.client.ClientMaster;
import com.magento.devsync.client.ClientPathResolver;
import com.magento.devsync.config.YamlFile;
import com.magento.devsync.filewatcher.ModifiedFileHistory;
import com.magento.devsync.server.ServerMaster;
import com.magento.devsync.server.ServerPathResolver;

/**
 * Background thread for receiving messages from the other endpoint.
 * Shared by both client and server as much of protocol is in common.
 * It reacts to requests initiated by a requestor on the other end of
 * the socket. Responses are sent back are encoded by the reactor as well.
 */
public class Reactor implements Runnable {

    private Logger logger;
    private ServerMaster server;
    private ClientMaster client;
    private PathResolver pathResolver;
    private boolean gracefulExit = false;
    private YamlFile config;
    private Channel channel;
    private FileOutputStream writeFileOutputStream;
    private FileChannel writeFileChannel;
    private File writeFileName;
    private boolean writeFileCanExecute;
    private ModifiedFileHistory modifiedFileLog;

    /**
     * Constructor used by client main program. In particular, the configuration file is known. 
     */
    public Reactor(Channel channel, ClientMaster client, YamlFile config, Logger logger, ModifiedFileHistory modifiedFileLog) {
        this.channel = channel;
        this.client = client;
        this.server = null;
        this.config = config;
        this.pathResolver = new ClientPathResolver();
        this.logger = logger;
        this.modifiedFileLog = modifiedFileLog;
    }

    /**
     * Constructor used by the server. It needs to establish socket connection with client before
     * the configuration file is known (the config comes from the client).
     */
    public Reactor(Channel channel, ServerMaster server, Logger logger, ModifiedFileHistory modifiedFileLog) {
        this.channel = channel;
        this.client = null;
        this.server = server;
        this.logger = logger;
        this.modifiedFileLog = modifiedFileLog;
    }

    /**
     * The path resolver for the server is not known until a message arrives
     * with the required configuration to do resolution. So this cannot be done
     * in constructor for the server case.
     */
    public void setConfig(YamlFile config) {
        this.config = config;
        this.pathResolver = new ServerPathResolver(config);
    }

    /**
     * Keep receiving messages until the socket is closed or asked to shut down.
     */
    public void run() {
        try {
            while (true) {
                MessageReader msg = channel.receive();
                if (msg == null) {
                    break;
                }
                processMessage(msg);
            }
        } catch (Exception e) {
            if (gracefulExit) {
                logger.log(e.getMessage());
            } else {
                e.printStackTrace();
            }
        }
    }

    private void processMessage(MessageReader msg) {

        try {
            // First byte is the 'command'.
            int command = msg.getByte();

            switch (command) {

            case ProtocolSpec.CHECK_PROTOCOL_VERSION: {
                log("REQU: Check protocol");
                int version = msg.getInt();
                if (version != ProtocolSpec.PROTOCOL_VERSION) {
                    respondNotOk("Client and server are at different protocol versions.");
                } else {
                    respondOk();
                }
                break;
            }

            case ProtocolSpec.SET_CONFIG: {
                // Send the whole YAML config file, which is a little
                // hacky (but easy to get going with).
                log("REQU: set config");
                try {
                    String yamlFile = msg.getString();
                    setConfig(YamlFile.parseYaml(yamlFile));
                    server.setConfig(config, pathResolver);
                    respondOk();
                } catch (Exception e) {
                    respondNotOk(e);
                }
                break;
            }

            case ProtocolSpec.ERROR_MESSAGE: {
                log("REQU: error");
                String message = msg.getString();
                log(message);
                respondOk();
                break;
            }

            case ProtocolSpec.START_SERVER_SYNC: {
                log("REQU: initial sync request");
                try {
                    server.initialSync();
                    respondOk();
                } catch (Exception e) {
                    respondNotOk(e);
                }
                break;
            }

            case ProtocolSpec.SERVER_SYNC_COMPLETE: {
                log("REQU: initial sync COMPLETE");
                try {
                    client.syncComplete();
                    respondOk();
                } catch (Exception e) {
                    respondNotOk(e);
                }
                break;
            }

            case ProtocolSpec.PATH_FINGERPRINT: { 
                log("REQU: fingerprint");
                try {
                    String path = msg.getString();
                    String fingerprint = msg.getString();
                    log("fingerprint path=" + path + " fingerprint=" + fingerprint);
                    pathFingerprint(path, fingerprint);
                } catch (Exception e) {
                    respondNotOk(e);
                }
                break;
            }

            case ProtocolSpec.PATH_DELETED: {
                log("REQU: delete path");
                try {
                    String path = msg.getString();
                    if (pathDeleted(path)) {
                        respondOk();
                    } else {
                        respondNotOk("Failed to delete " + path);
                    }
                } catch (Exception e) {
                    respondNotOk(e);
                }
                break;
            }

            case ProtocolSpec.WRITE_FILE: {
                log("REQU: write file");
                String path = msg.getString();
                writeFileCanExecute = msg.getBoolean();

                try {
                    modifiedFileLog.startingToWrite(path);
                    writeFileName = pathResolver.clientPathToFile(path);
                    logger.log("WriteFile, going to write to " + writeFileName);
                    writeFileOutputStream = new FileOutputStream(writeFileName, false);
                    writeFileChannel = writeFileOutputStream.getChannel();
                    logger.log("File opened for writing");
                    processWriteMessage(msg);
                } catch (Exception e) {
                    respondNotOk(e);
                }
                break;
            }

            case ProtocolSpec.MORE_DATA: {
                processWriteMessage(msg);
                break;
            }

            case ProtocolSpec.CREATE_DIRECTORY: {
                log("REQU: create directory");
                String path = msg.getString();
                createDirectory(path);
                break;
            }

            default: {
                log("RECV: UNKNOWN COMMAND! " + command);
                System.exit(1);
                break;
            }
            }

        } catch (Exception e) {
            //			log(e.getMessage());
            e.printStackTrace();
        }
    }

    private int writeAll(FileChannel writeFileChannel, ByteBuffer buf, int bufLen) throws IOException {
        int remaining = bufLen;
        int totalWritten = 0;
        int basePosition = buf.position();
        while (true) {
            int bytesWritten = writeFileChannel.write(buf);
            if (bytesWritten < 0) {
                return totalWritten;
            }
            totalWritten += bytesWritten;
            remaining -= bytesWritten;
            if (remaining == 0) {
                return totalWritten;
            }
            buf.position(basePosition + totalWritten);
        }
    }

    private void processWriteMessage(MessageReader msg) throws IOException {

        boolean eof = msg.getBoolean();
        int length = msg.getInt();
        ByteBuffer copyBuf = msg.getBytes(length);

        try {
            writeAll(writeFileChannel, copyBuf, length);
            respondOk();
        } catch (Exception e) {
            respondNotOk(e);
        }

        if (eof) {
            closeWritingFile();
        }

    }

    private void closeWritingFile() throws IOException {
        writeFileChannel.close();
        writeFileOutputStream.close();
        if (writeFileCanExecute) {
            writeFileName.setExecutable(true);
        }
        modifiedFileLog.writingCompleted();
    }

    private void log(String message) {
        logger.log(message);
    }

    private boolean pathDeleted(String path) throws IOException {
        File localPath = pathResolver.clientPathToFile(path);
        if (Files.isDirectory(localPath.toPath(), LinkOption.NOFOLLOW_LINKS)) {

            // Recursive delete directory.
            Path directory = localPath.toPath();
            Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });

        } else {
            try {
                Files.delete(localPath.toPath());
            } catch (NoSuchFileException e) {
                // Ignore 'no such file' exceptions.
                // In 'sync' mode both server and client might bounce
                // delete message back to original source.
            }
        }
        return true;
    }

    private void pathFingerprint(final String path, final String remoteFingerprint) throws IOException {
        File localPath = pathResolver.clientPathToFile(path);
        logger.log(". fingerprint " + path + " => " + localPath);
        if (localPath.exists()) {
            if (localPath.isDirectory()) {
                if (server != null) {
                    try {
                        respondNotOk("Cannot sync " + path + " as one is file, other is directory");
                        return;
                    } catch (IOException e) {
                        respondNotOk(e);
                        return;
                    }
                } else {
                    respondNotOk("Cannot sync " + path + " as one is file, other is directory");
                    return;
                }
            } else {
                String localFingerprint = PathResolver.fingerprint(localPath);
                if (localFingerprint.equals(remoteFingerprint)) {
                    logger.log(". fingerprints match - do not copy " + path);
                    respondOk();
                    return;
                }
            }
        }

        // The file is different - please send us a copy!
        logger.log("RESP: SEND-ME-FILE: " + path);
        try {
            logger.log(". fingerprints don't match - REQUEST A COPY " + path);
            MessageWriter msg = new MessageWriter();
            msg.putByte(ProtocolSpec.SEND_ME_FILE);
            msg.putString(path);
            channel.send(msg);
        } catch (IOException e) {
            respondNotOk(e);
        }
    }

    private void createDirectory(String path) throws IOException {
        File f = pathResolver.clientPathToFile(path);
        if (f.isDirectory()) {
            // Already exists as directory
            respondOk();
        } else if (f.exists()) {
            // Path exists, but as a file!
            respondNotOk("Unable to sync directory " + path + " as it already exists as a file.");
        } else {
            if (f.mkdir()) {
                respondOk();
            } else {
                // Mkdir failed!
                respondNotOk("Failed to create directory " + path);
            }
        }
    }

    private void respondOk() throws IOException {
        logger.log("RESP: OK");
        MessageWriter msg = new MessageWriter();
        msg.putByte(ProtocolSpec.OK);
        channel.send(msg);
    }

    private void respondNotOk(String message) throws IOException {
        logger.log("RESP: NOT_OK: " + message);
        MessageWriter msg = new MessageWriter();
        msg.putByte(ProtocolSpec.NOT_OK);
        msg.putString(message);
        channel.send(msg);
    }

    private void respondNotOk(Exception e) throws IOException {
        logger.log("RESP: NOT_OK: (internal error) " + e.getMessage());
        e.printStackTrace();
        MessageWriter msg = new MessageWriter();
        msg.putByte(ProtocolSpec.NOT_OK);
        msg.putString(e.getMessage());
        channel.send(msg);
    }
}

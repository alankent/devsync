package com.magento.devsync;

import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import com.magento.devsync.client.ClientMaster;
import com.magento.devsync.client.ClientSlave;
import com.magento.devsync.communications.Channel;
import com.magento.devsync.communications.ChannelMultiplexer;
import com.magento.devsync.communications.Logger;
import com.magento.devsync.communications.Requestor;
import com.magento.devsync.config.YamlFile;
import com.magento.devsync.filewatcher.ModifiedFileHistory;


/**
 * Client application main program entry point.
 */
public class DevsyncClientMain {

    public static final String VERSION = "0.1";

    private static Logger logger;

    public static void main(String[] args) {

        logger = new Logger("C");

        try {
            logger.log("Devsync version " + VERSION + "\n");
            byte[] encoded = Files.readAllBytes(Paths.get(getConfigFile()));
            String yamlContents = new String(encoded, StandardCharsets.UTF_8);
            YamlFile config = YamlFile.parseYaml(yamlContents);

            logger.log("* Connecting to server\n");

            String host = getHost();
            int portNum = getPort();

            try (Socket sock = new Socket(host, portNum)) {
                logger.log("Connected to server.");

                ChannelMultiplexer multiplexer = new ChannelMultiplexer(sock, logger);
                Channel toServerChannel = new Channel(0, multiplexer);
                Channel fromServerChannel = new Channel(1, multiplexer);
                new Thread(multiplexer, "Client-Multiplexer").start();

                ModifiedFileHistory history = new ModifiedFileHistory();

                ClientMaster master = new ClientMaster(toServerChannel, config, logger, history);

                ClientSlave slave = new ClientSlave(fromServerChannel, config, logger, master, history);
                new Thread(slave, "Client-Slave").start();

                master.run();

            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getHost() {
        String host = System.getenv("DEVSYNC_HOST");
        if (host == null || host.equals("")) {
            host = System.getProperty("devsync.host");
            if (host == null || host.equals("")) {
                logger.log("Environment variable DEVSYNC_HOST is not set.");
                System.exit(1);
            }
        }
        return host;
    }

    public static int getPort() {
        String port = System.getenv("DEVSYNC_PORT");
        if (port == null || port.equals("")) {
            port = System.getProperty("devsync.port");
            if (port == null || port.equals("")) {
                throw new RuntimeException("Environment variable DEVSYNC_PORT is not set.");
            }
        }
        try {
            return Integer.parseInt(port);
        } catch (NumberFormatException e) {
            throw new RuntimeException("Environment variable DEVSYNC_PORT must be a number");
        }
    }

    public static String getConfigFile() {
        String f = System.getenv("DEVSYNC_CONFIG");
        if (f != null && f.length() > 0) {
            return f;
        }
        f = System.getProperty("devsync.config");
        if (f != null && f.length() > 0) {
            return f;
        }
        f = ".devsync.yml";
        if (new File(f).exists()) {
            return f;
        }
        f = ".devsync.yaml";
        if (new File(f).exists()) {
            return f;
        }
        throw new RuntimeException("Unable to find .devsync.yml configuration file.");
    }
}

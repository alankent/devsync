package com.magento.devsync;

import java.io.File;
import java.net.ConnectException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import com.magento.devsync.client.ClientMaster;
import com.magento.devsync.communications.Channel;
import com.magento.devsync.communications.ChannelMultiplexer;
import com.magento.devsync.communications.Logger;
import com.magento.devsync.communications.Reactor;
import com.magento.devsync.config.YamlFile;
import com.magento.devsync.filewatcher.ModifiedFileHistory;


/**
 * Client application main program entry point.
 */
public class DevsyncClientMain {

    public static final String VERSION = "0.1";

    private static Logger logger;

    public static void main(String[] args) {

        boolean quietMode = false;
        boolean debugMode = false;
        boolean verboseMode = false;
        boolean forceProjectInitialization = false;
        
        int arg = 0;
        while (arg < args.length) {
            if (args[arg].equals("--quiet")) {
                quietMode = true;
                arg++;
            } else if (args[arg].equals("--debug")) {
                debugMode = true;
                arg++;
            } else if (args[arg].equals("--verbose")) {
                verboseMode = true;
                arg++;
            } else if (args[arg].equals("--init-project")) {
                forceProjectInitialization = true;
                arg++;
            } else {
                System.err.println("Unknown command line option '" + args[arg] + "'.");
                System.exit(1);
            }
        }
        
        logger = new Logger("C", quietMode, debugMode, verboseMode);

        try {
            logger.info("Devsync version " + VERSION + "\n");

            logger.infoVerbose("* Connecting to server\n");

            String host = getHost();
            int portNum = getPort();

            try (Socket sock = new Socket(host, portNum)) {
                logger.info("Connected to server.");

                ChannelMultiplexer multiplexer = new ChannelMultiplexer(sock);
                Channel toServerChannel = new Channel(0, multiplexer);
                Channel fromServerChannel = new Channel(1, multiplexer);
                new Thread(multiplexer, "Client-Multiplexer").start();

                ModifiedFileHistory history = new ModifiedFileHistory();

                // We are going to start communicating before loading config file,
                // as we may download it from the server.
                Reactor slave = new Reactor(fromServerChannel, logger, history);
                new Thread(slave, "Client-Slave").start();

                ClientMaster master = new ClientMaster(toServerChannel, logger, history);
                master.preConfigHandshake(forceProjectInitialization);

                // Load up configuration file.
                byte[] encoded = Files.readAllBytes(Paths.get(getConfigFile()));
                String yamlContents = new String(encoded, StandardCharsets.UTF_8);
                YamlFile config = YamlFile.parseYaml(yamlContents);
                master.setConfig(config);

                slave.setClientMaster(master);
                master.run();

            } catch (ConnectException e) {
                logger.warn("Failed to connect to server.");
                logger.debug(e);
            } catch (Exception e) {
                logger.warn(e);
            }
        } catch (Exception e) {
            logger.warn(e);
        }
    }

    public static String getHost() {
        String host = System.getenv("DEVSYNC_HOST");
        if (host == null || host.equals("")) {
            host = System.getProperty("devsync.host");
            if (host == null || host.equals("")) {
                logger.warn("Environment variable DEVSYNC_HOST is not set.");
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
                logger.warn("Environment variable DEVSYNC_PORT is not set.");
                System.exit(1);
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

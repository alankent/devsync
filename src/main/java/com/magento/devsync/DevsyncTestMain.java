package com.magento.devsync;

import java.io.IOException;

/**
 * This main makes it easier to debug - having client and server processes
 * both running in one instance. You would never do this in real life.
 */
public class DevsyncTestMain {
    
    public static void main(String[] args) {
        startServerMain();
        DevsyncClientMain.main(args);
    }

    private static void startServerMain() {
        // Kick off server to make debugging easier during development.
        boolean debugging = true;
        if (debugging) {
            try {
                Thread serverThread = new Thread(new Runnable() { 
                    public void run() {
                        try {
                            DevsyncServerMain.main(new String[] {
                                    "--port",
                                    Integer.toString(DevsyncClientMain.getPort()),
                                    "--template",
                                    "foo/template"
                            });
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }, "Server-Main-Program");
                serverThread.setDaemon(true);
                serverThread.start();
                Thread.sleep(1);
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        }
    }
}

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
                new Thread(new Runnable() { 
                    public void run() {
                        try {
                            DevsyncServerMain.main(new String[] {
                                    Integer.toString(DevsyncClientMain.getPort())
                            });
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }, "Server-Main-Program").start();
                Thread.sleep(1);
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        }
    }
}

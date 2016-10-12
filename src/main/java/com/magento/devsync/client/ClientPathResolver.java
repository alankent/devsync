package com.magento.devsync.client;

import java.io.File;

import com.magento.devsync.communications.PathResolver;

public class ClientPathResolver extends PathResolver {

    private String homeDirectory;

    public ClientPathResolver() {
        homeDirectory = System.getProperty("user.home");
        if (homeDirectory == null) {
            homeDirectory = System.getenv("HOME");
            if (homeDirectory == null || homeDirectory.length() == 0) {
                System.out.println("Unable to determine user's home directory.");
                System.exit(1);
            }
        }
    }

    @Override
    public File clientPathToFile(String abstractPath) {
        String p = abstractPath;
        if (p.length() > 0 && p.charAt(0) == '~') {
            p = homeDirectory + p.substring(1);
        }
        return new File(p);
    }
}

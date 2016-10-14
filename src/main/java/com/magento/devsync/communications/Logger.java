package com.magento.devsync.communications;

public class Logger {

    private String prefix;
    private boolean quiet;
    private boolean debug;
    private boolean verbose;

    public Logger(String prefix, boolean quiet, boolean debug, boolean verbose) {
        this.prefix = prefix;
        this.quiet = quiet;
        this.debug = debug;
        this.verbose = verbose;
    }

    private void log(String message) {
        if (debug) {
            System.out.println(prefix + ":" + message);
        } else {
            System.out.println(message);
        }
        System.out.flush();
    }

    public void info(String msg) {
        if (debug) {
            log("INFO: " + msg);
        } else if (!quiet) {
            log(msg);
        }
    }

    public void infoVerbose(String msg) {
        if (verbose) {
            info(msg);
        }
    }

    public void info(Exception e) {
        info(e.getMessage());
        if (debug) {
            e.printStackTrace();
        }
    }

    public void infoVerbose(Exception e) {
        if (verbose) {
            info(e.getMessage());
        }
        if (debug) {
            e.printStackTrace();
        }
    }

    public void debug(String msg) {
        if (debug) {
            log("DEBUG: " + msg);
        }
    }

    public void debugVerbose(String msg) {
        if (verbose) {
            debug(msg);
        }
    }

    public void debug(Exception e) {
        if (debug) {
            log("DEBUG: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void warn(String msg) {
        if (debug) {
            log("WARN: " + msg);
        } else {
            log(msg);
        }
    }

    public void warn(Exception e) {
        if (debug) {
            log("WARN: " + e.getMessage());
        } else {
            log(e.getMessage());
        }
        e.printStackTrace();
    }
}

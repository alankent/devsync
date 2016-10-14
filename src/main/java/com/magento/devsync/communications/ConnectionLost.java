package com.magento.devsync.communications;

public class ConnectionLost extends Exception {

    private static final long serialVersionUID = -4418607727729048762L;
    
    public ConnectionLost(Exception e) {
        super(e);
    }

}

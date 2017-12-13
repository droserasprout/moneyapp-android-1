package com.cactusteam.money.sync;

/**
 * @author vpotapenko
 */
public class SyncException extends Exception{

    public SyncException(String message) {
        super(message);
    }

    public SyncException(String message, Throwable cause) {
        super(message, cause);
    }
}

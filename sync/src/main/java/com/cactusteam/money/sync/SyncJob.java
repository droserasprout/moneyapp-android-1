package com.cactusteam.money.sync;

/**
 * @author vpotapenko
 */
public abstract class SyncJob<T> {

    public boolean error;
    public String errorMessage;

    public T execute() {
        error = false;
        try {
            return doJob();
        } catch (Exception e) {
            e.printStackTrace();

            error = true;
            errorMessage = e.getMessage();

            return null;
        }
    }

    protected abstract T doJob() throws Exception;
}

package com.cactusteam.money.sync.changes;

/**
 * @author vpotapenko
 */
public interface IChangesStorage {

    void initialize() throws Exception;

    boolean lock(String name, String lockId) throws Exception;

    void unlock(String name, String lockId) throws Exception;

    String getLockId(String name) throws Exception;

    void saveLog(ChangesList log, String name) throws Exception;

    ChangesList getLog(String name) throws Exception;
}

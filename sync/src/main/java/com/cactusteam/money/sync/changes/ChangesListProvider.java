package com.cactusteam.money.sync.changes;

import java.util.Collections;

/**
 * @author vpotapenko
 */
public class ChangesListProvider {

    private static final String PREVIOUS_LOG_NAME_PATTERN = "%s.%d.ma";
    private static final String LOG_NAME = "%s.ma";
    private static final String LOCK_NAME = "%s.lock";

    private static final int MAX_LOG_ITEMS = 1000;

    private final String logPrefix;
    private final IChangesStorage logStorage;

    public ChangesListProvider(String logPrefix, IChangesStorage logStorage) {
        this.logPrefix = logPrefix;
        this.logStorage = logStorage;
    }

    public boolean lock(String lockId) throws Exception {
        return logStorage.lock(getLockName(), lockId);
    }

    private String getLockName() {
        return String.format(LOCK_NAME, logPrefix);
    }

    public void unlock(String lockId) throws Exception {
        logStorage.unlock(getLockName(), lockId);
    }

    public String getLockId() throws Exception {
        return logStorage.getLockId(getLockName());
    }

    public ChangesList getCurrentList() throws Exception {
        ChangesList log = logStorage.getLog(getCurrentLogName());
        return log == null ? new ChangesList() : log;
    }

    private String getCurrentLogName() {
        return String.format(LOG_NAME, logPrefix);
    }

    public ChangesList getPreviousLog(int version) throws Exception {
        return logStorage.getLog(getPreviousLogName(version));
    }

    private String getPreviousLogName(int version) {
        return String.format(PREVIOUS_LOG_NAME_PATTERN, logPrefix, version);
    }

    public void putLog(ChangesList log) throws Exception {
        if (log.items.size() > MAX_LOG_ITEMS) {
            rotateLog(log);
        } else {
            logStorage.saveLog(log, getCurrentLogName());
        }
    }

    private void rotateLog(ChangesList log) throws Exception {
        ChangesList newLog = new ChangesList();
        while (log.items.size() > MAX_LOG_ITEMS) {
            newLog.items.add(log.items.remove(log.items.size() - 1));
        }
        logStorage.saveLog(log, getPreviousLogName(log.version));

        Collections.reverse(newLog.items);
        newLog.version = log.version + 1;
        newLog.previousVersion = log.version;
        putLog(newLog);
    }
}

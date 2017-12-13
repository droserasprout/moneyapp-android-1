package com.cactusteam.money.sync.stubs;

import com.cactusteam.money.sync.changes.ChangesList;
import com.cactusteam.money.sync.changes.ChangesListFactory;
import com.cactusteam.money.sync.changes.IChangesStorage;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * @author vpotapenko
 */
public class StubChangesStorage implements IChangesStorage {

    private static final String FOLDER_NAME = "testSyncFolder";

    private final File folder;

    protected final ChangesListFactory logFactory = new ChangesListFactory();

    public StubChangesStorage() throws IOException {
        folder = new File(FOLDER_NAME);
        FileUtils.forceMkdir(folder);

        logFactory.setFormattedOutput(true);
    }

    public void clearTestFolder() {
        FileUtils.deleteQuietly(folder);
    }

    public String getContent(String name) throws IOException {
        File log = new File(folder, name);
        return FileUtils.readFileToString(log);
    }

    public void setContent(String name, String content) throws IOException {
        File log = new File(folder, name);
        FileUtils.write(log, content);
    }

    @Override
    public void initialize() throws Exception {
        // do nothing
    }

    @Override
    public boolean lock(String name, String lockId) throws Exception {
        File lockFile = new File(folder, name);
        if (lockFile.exists()) return false;

        FileUtils.write(lockFile, lockId);
        return true;
    }

    @Override
    public void unlock(String name, String lockId) throws Exception {
        File lockFile = new File(folder, name);
        if (!lockFile.exists()) return;

        String content = FileUtils.readFileToString(lockFile);
        if (content.equals(lockId)) {
            FileUtils.forceDelete(lockFile);
        }
    }

    @Override
    public String getLockId(String name) throws Exception {
        return null;
    }

    @Override
    public void saveLog(ChangesList log, String name) throws Exception {
        File logFile = new File(folder, name);
        logFactory.write(new FileOutputStream(logFile), log);
    }

    @Override
    public ChangesList getLog(String name) throws Exception {
        File log = new File(folder, name);
        if (log.exists()) {
            return logFactory.read(new FileInputStream(log));
        } else {
            return null;
        }
    }
}

package com.cactusteam.money.sync;

import com.cactusteam.money.sync.changes.ChangeItem;
import com.cactusteam.money.sync.changes.ChangesApplier;
import com.cactusteam.money.sync.changes.ChangesList;
import com.cactusteam.money.sync.changes.ChangesListProvider;
import com.cactusteam.money.sync.changes.IChangesStorage;
import com.cactusteam.money.sync.changes.ObjectWrapper;
import com.cactusteam.money.sync.model.SyncAccount;
import com.cactusteam.money.sync.model.SyncBudget;
import com.cactusteam.money.sync.model.SyncCategory;
import com.cactusteam.money.sync.model.SyncDebt;
import com.cactusteam.money.sync.model.SyncDebtNote;
import com.cactusteam.money.sync.model.SyncObject;
import com.cactusteam.money.sync.model.SyncPattern;
import com.cactusteam.money.sync.model.SyncSubcategory;
import com.cactusteam.money.sync.model.SyncTransaction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * @author vpotapenko
 */
public class SyncJobsExecutor {

    private static final int LOCK_TRIES = 10;
    private static final int TIMEOUT_MILLIS = 5000;

    private final Map<Integer, ChangesListProvider> listProviders = new HashMap<>();

    private final IProxyDatabase proxyDatabase;
    private final ILogger logger;

    private String deviceId;

    private ChangesApplier changesApplier;

    public SyncJobsExecutor(IProxyDatabase proxyDatabase,
                            IChangesStorage logStorage,
                            ILogger logger,
                            String deviceId) {
        this.proxyDatabase = proxyDatabase;
        this.logger = logger;
        this.deviceId = deviceId;

        listProviders.put(SyncConstants.ACCOUNT_TYPE, new ChangesListProvider("accounts", logStorage));
        listProviders.put(SyncConstants.CATEGORY_TYPE, new ChangesListProvider("categories", logStorage));
        listProviders.put(SyncConstants.SUBCATEGORY_TYPE, new ChangesListProvider("subcategories", logStorage));
        listProviders.put(SyncConstants.DEBT_TYPE, new ChangesListProvider("debts", logStorage));
        listProviders.put(SyncConstants.DEBT_NOTE_TYPE, new ChangesListProvider("debt_notes", logStorage));
        listProviders.put(SyncConstants.TRANSACTION_TYPE, new ChangesListProvider("transactions", logStorage));
        listProviders.put(SyncConstants.PATTERN_TYPE, new ChangesListProvider("patterns", logStorage));
        listProviders.put(SyncConstants.BUDGET_TYPE, new ChangesListProvider("budgets", logStorage));
    }

    public List<SyncJob<Boolean>> createCheckJobs() {
        List<SyncJob<Boolean>> jobs = new LinkedList<>();
        jobs.add(new SyncJob<Boolean>() {
            @Override
            protected Boolean doJob() throws Exception {
                return proxyDatabase.hasDirtyAccounts() ||
                        checkListItems(SyncConstants.ACCOUNT_TYPE);
            }
        });
        jobs.add(new SyncJob<Boolean>() {
            @Override
            protected Boolean doJob() throws Exception {
                return proxyDatabase.hasDirtyCategories() ||
                        checkListItems(SyncConstants.CATEGORY_TYPE);
            }
        });
        jobs.add(new SyncJob<Boolean>() {
            @Override
            protected Boolean doJob() throws Exception {
                return proxyDatabase.hasDirtySubcategories() ||
                        checkListItems(SyncConstants.SUBCATEGORY_TYPE);
            }
        });
        jobs.add(new SyncJob<Boolean>() {
            @Override
            protected Boolean doJob() throws Exception {
                return proxyDatabase.hasDirtyDebts() ||
                        checkListItems(SyncConstants.DEBT_TYPE);
            }
        });
        jobs.add(new SyncJob<Boolean>() {
            @Override
            protected Boolean doJob() throws Exception {
                return proxyDatabase.hasDirtyDebtNotes() ||
                        checkListItems(SyncConstants.DEBT_NOTE_TYPE);
            }
        });
        jobs.add(new SyncJob<Boolean>() {
            @Override
            protected Boolean doJob() throws Exception {
                return proxyDatabase.hasDirtyTransactions() ||
                        checkListItems(SyncConstants.TRANSACTION_TYPE);
            }
        });
        jobs.add(new SyncJob<Boolean>() {
            @Override
            protected Boolean doJob() throws Exception {
                return proxyDatabase.hasDirtyPatterns() ||
                        checkListItems(SyncConstants.PATTERN_TYPE);
            }
        });
        jobs.add(new SyncJob<Boolean>() {
            @Override
            protected Boolean doJob() throws Exception {
                return proxyDatabase.hasDirtyBudgets() ||
                        checkListItems(SyncConstants.BUDGET_TYPE);
            }
        });

        return jobs;
    }

    public List<SyncJob<Object>> createSyncJobs() {
        // order is important, sync objects have dependencies
        List<SyncJob<Object>> jobs = new LinkedList<>();
        jobs.add(new SyncJob<Object>() {
            @Override
            protected Object doJob() throws Exception {
                syncAccounts();
                return null;
            }
        });
        jobs.add(new SyncJob<Object>() {
            @Override
            protected Object doJob() throws Exception {
                syncCategories();
                return null;
            }
        });
        jobs.add(new SyncJob<Object>() {
            @Override
            protected Object doJob() throws Exception {
                syncSubcategories();
                return null;
            }
        });
        jobs.add(new SyncJob<Object>() {
            @Override
            protected Object doJob() throws Exception {
                syncDebts();
                return null;
            }
        });
        jobs.add(new SyncJob<Object>() {
            @Override
            protected Object doJob() throws Exception {
                syncDebtNotes();
                return null;
            }
        });
        jobs.add(new SyncJob<Object>() {
            @Override
            protected Object doJob() throws Exception {
                syncBudgets();
                return null;
            }
        });
        jobs.add(new SyncJob<Object>() {
            @Override
            protected Object doJob() throws Exception {
                syncTransactions();
                return null;
            }
        });
        jobs.add(new SyncJob<Object>() {
            @Override
            protected Object doJob() throws Exception {
                syncPatterns();
                return null;
            }
        });

        return jobs;
    }

    private boolean checkListItems(int type) throws Exception {
        ChangesListProvider changesListProvider = listProviders.get(type);

        String lockId = generateLockId();
        boolean locked = changesListProvider.lock(lockId);
        for (int i = 0; i < LOCK_TRIES && !locked; i++) {
            Thread.sleep(TIMEOUT_MILLIS);
            locked = changesListProvider.lock(lockId);
        }
        try {
            ChangesList log = changesListProvider.getCurrentList();
            if (log == null) return false;

            for (ChangeItem item : log.items) {
                if (!proxyDatabase.alreadyApplied(type, item.id)) return true;
            }
        } finally {
            changesListProvider.unlock(lockId);
        }

        return false;
    }

    void syncBudgets() throws Exception {
        int type = SyncConstants.BUDGET_TYPE;
        ChangesListProvider changesListProvider = listProviders.get(type);
        String lockId = lockJournal(type, changesListProvider);
        try {
            downloadBudgets();
            uploadBudgets();
        } finally {
            changesListProvider.unlock(lockId);
        }
    }

    void uploadBudgets() throws Exception {
        ChangesListProvider changesListProvider = listProviders.get(SyncConstants.BUDGET_TYPE);
        ChangesList currentList = changesListProvider.getCurrentList();

        List<SyncBudget> dirtyBudgets = proxyDatabase.getDirtyBudgets(currentList);
        if (dirtyBudgets.isEmpty()) return;

        uploadDirtyObjects(dirtyBudgets, SyncConstants.BUDGET_TYPE, currentList, "budgets");
    }

    void downloadBudgets() throws Exception {
        ChangesListProvider changesListProvider = listProviders.get(SyncConstants.BUDGET_TYPE);
        List<ChangeItem> items = downloadItems(changesListProvider, "budgets");
        getChangesApplier().execute(proxyDatabase, items);
    }

    void syncPatterns() throws Exception {
        int type = SyncConstants.PATTERN_TYPE;
        ChangesListProvider changesListProvider = listProviders.get(type);
        String lockId = lockJournal(type, changesListProvider);
        try {
            downloadPatterns();
            uploadPatterns();
        } finally {
            changesListProvider.unlock(lockId);
        }
    }

    void uploadPatterns() throws Exception {
        List<SyncPattern> dirtyPatterns = proxyDatabase.getDirtyPatterns();
        if (dirtyPatterns.isEmpty()) return;

        ChangesListProvider changesListProvider = listProviders.get(SyncConstants.PATTERN_TYPE);
        ChangesList list = changesListProvider.getCurrentList();

        uploadDirtyObjects(dirtyPatterns, SyncConstants.PATTERN_TYPE, list, "patterns");
    }

    void downloadPatterns() throws Exception {
        ChangesListProvider changesListProvider = listProviders.get(SyncConstants.PATTERN_TYPE);
        List<ChangeItem> items = downloadItems(changesListProvider, "patterns");
        getChangesApplier().execute(proxyDatabase, items);
    }

    void syncDebts() throws Exception {
        int type = SyncConstants.DEBT_TYPE;
        ChangesListProvider changesListProvider = listProviders.get(type);
        String lockId = lockJournal(type, changesListProvider);
        try {
            downloadDebts();
            uploadDebts();
        } finally {
            changesListProvider.unlock(lockId);
        }
    }

    void uploadDebts() throws Exception {
        List<SyncDebt> dirtyDebts = proxyDatabase.getDirtyDebts();
        if (dirtyDebts.isEmpty()) return;

        ChangesListProvider changesListProvider = listProviders.get(SyncConstants.DEBT_TYPE);
        ChangesList list = changesListProvider.getCurrentList();
        uploadDirtyObjects(dirtyDebts, SyncConstants.DEBT_TYPE, list, "debts");
    }

    void downloadDebts() throws Exception {
        ChangesListProvider changesListProvider = listProviders.get(SyncConstants.DEBT_TYPE);
        List<ChangeItem> items = downloadItems(changesListProvider, "debts");
        getChangesApplier().execute(proxyDatabase, items);
    }

    void syncDebtNotes() throws Exception {
        int type = SyncConstants.DEBT_NOTE_TYPE;
        ChangesListProvider changesListProvider = listProviders.get(type);
        String lockId = lockJournal(type, changesListProvider);
        try {
            downloadDebtNotes();
            uploadDebtNotes();
        } finally {
            changesListProvider.unlock(lockId);
        }
    }

    void uploadDebtNotes() throws Exception {
        List<SyncDebtNote> dirtyDebtNotes = proxyDatabase.getDirtyDebtNotes();
        if (dirtyDebtNotes.isEmpty()) return;

        ChangesListProvider changesListProvider = listProviders.get(SyncConstants.DEBT_NOTE_TYPE);
        ChangesList list = changesListProvider.getCurrentList();
        uploadDirtyObjects(dirtyDebtNotes, SyncConstants.DEBT_NOTE_TYPE, list, "debt_notes");
    }

    void downloadDebtNotes() throws Exception {
        ChangesListProvider changesListProvider = listProviders.get(SyncConstants.DEBT_NOTE_TYPE);
        List<ChangeItem> items = downloadItems(changesListProvider, "debt_notes");
        getChangesApplier().execute(proxyDatabase, items);
    }

    private ChangesApplier getChangesApplier() {
        if (changesApplier == null) {
            changesApplier = new ChangesApplier(deviceId);
        }
        return changesApplier;
    }

    private void syncTransactions() throws Exception {
        int type = SyncConstants.TRANSACTION_TYPE;
        ChangesListProvider changesListProvider = listProviders.get(type);
        String lockId = lockJournal(type, changesListProvider);
        try {
            downloadTransactions();
            uploadTransactions();
        } finally {
            changesListProvider.unlock(lockId);
        }
    }

    void uploadTransactions() throws Exception {
        List<SyncTransaction> dirtyTransactions = proxyDatabase.getDirtyTransactions();
        if (dirtyTransactions.isEmpty()) return;

        ChangesListProvider changesListProvider = listProviders.get(SyncConstants.TRANSACTION_TYPE);
        ChangesList list = changesListProvider.getCurrentList();

        uploadDirtyObjects(dirtyTransactions, SyncConstants.TRANSACTION_TYPE, list, "transactions");
    }

    void downloadTransactions() throws Exception {
        ChangesListProvider changesListProvider = listProviders.get(SyncConstants.TRANSACTION_TYPE);
        List<ChangeItem> items = downloadItems(changesListProvider, "transactions");
        getChangesApplier().execute(proxyDatabase, items);
    }

    private void syncSubcategories() throws Exception {
        int type = SyncConstants.SUBCATEGORY_TYPE;
        ChangesListProvider changesListProvider = listProviders.get(type);
        String lockId = lockJournal(type, changesListProvider);
        try {
            downloadSubcategories();
            uploadSubcategories();
        } finally {
            changesListProvider.unlock(lockId);
        }
    }

    void uploadSubcategories() throws Exception {
        List<SyncSubcategory> dirtySubcategories = proxyDatabase.getDirtySubcategories();
        if (dirtySubcategories.isEmpty()) return;

        ChangesListProvider changesListProvider = listProviders.get(SyncConstants.SUBCATEGORY_TYPE);
        ChangesList list = changesListProvider.getCurrentList();

        uploadDirtyObjects(dirtySubcategories, SyncConstants.SUBCATEGORY_TYPE, list, "subcategories");
    }

    void downloadSubcategories() throws Exception {
        ChangesListProvider changesListProvider = listProviders.get(SyncConstants.SUBCATEGORY_TYPE);
        List<ChangeItem> items = downloadItems(changesListProvider, "subcategories");
        getChangesApplier().execute(proxyDatabase, items);
    }

    private void syncCategories() throws Exception {
        int type = SyncConstants.CATEGORY_TYPE;
        ChangesListProvider changesListProvider = listProviders.get(type);
        String lockId = lockJournal(type, changesListProvider);
        try {
            downloadCategories();
            uploadCategories();
        } finally {
            changesListProvider.unlock(lockId);
        }
    }

    void uploadCategories() throws Exception {
        List<SyncCategory> dirtyCategories = proxyDatabase.getDirtyCategories();
        if (dirtyCategories.isEmpty()) return;

        ChangesListProvider changesListProvider = listProviders.get(SyncConstants.CATEGORY_TYPE);
        ChangesList list = changesListProvider.getCurrentList();
        uploadDirtyObjects(dirtyCategories, SyncConstants.CATEGORY_TYPE, list, "categories");
    }

    void downloadCategories() throws Exception {
        ChangesListProvider changesListProvider = listProviders.get(SyncConstants.CATEGORY_TYPE);
        List<ChangeItem> items = downloadItems(changesListProvider, "categories");
        getChangesApplier().execute(proxyDatabase, items);
    }

    private void syncAccounts() throws Exception {
        int type = SyncConstants.ACCOUNT_TYPE;
        ChangesListProvider changesListProvider = listProviders.get(type);
        String lockId = lockJournal(type, changesListProvider);
        try {
            downloadAccounts();
            uploadAccounts();
        } finally {
            changesListProvider.unlock(lockId);
        }
    }

    private String lockJournal(int type, ChangesListProvider changesListProvider) throws Exception {
        String lockId = generateLockId();

        boolean locked = changesListProvider.lock(lockId);
        for (int i = 0; i < LOCK_TRIES && !locked; i++) {
            String remoteLock = changesListProvider.getLockId();
            if (proxyDatabase.isOutdatedLock(remoteLock, type)) {
                changesListProvider.unlock(remoteLock);
            } else {
                Thread.sleep(TIMEOUT_MILLIS);
            }

            locked = changesListProvider.lock(lockId);
        }
        if (!locked) {
            String remoteLock = changesListProvider.getLockId();
            proxyDatabase.saveLock(remoteLock, type);

            switch (type) {
                case SyncConstants.ACCOUNT_TYPE:
                    throw new SyncException("Accounts journal is locked by another session. Try later.");
                case SyncConstants.CATEGORY_TYPE:
                    throw new SyncException("Categories journal is locked by another session. Try later.");
                case SyncConstants.SUBCATEGORY_TYPE:
                    throw new SyncException("Subcategories journal is locked by another session. Try later.");
                case SyncConstants.DEBT_TYPE:
                    throw new SyncException("Debts journal is locked by another session. Try later.");
                case SyncConstants.DEBT_NOTE_TYPE:
                    throw new SyncException("Debt notes journal is locked by another session. Try later.");
                case SyncConstants.TRANSACTION_TYPE:
                    throw new SyncException("Transactions journal is locked by another session. Try later.");
                case SyncConstants.PATTERN_TYPE:
                    throw new SyncException("Patterns journal is locked by another session. Try later.");
                case SyncConstants.BUDGET_TYPE:
                    throw new SyncException("Budgets journal is locked by another session. Try later.");
            }
        }
        return lockId;
    }

    void downloadAccounts() throws Exception {
        ChangesListProvider changesListProvider = listProviders.get(SyncConstants.ACCOUNT_TYPE);
        List<ChangeItem> items = downloadItems(changesListProvider, "accounts");
        getChangesApplier().execute(proxyDatabase, items);
    }

    private List<ChangeItem> downloadItems(ChangesListProvider changesListProvider, String logSuffix) throws Exception {
        logger.print("Downloading " + logSuffix);
        List<ChangeItem> result = new LinkedList<>();
        ChangesList log = changesListProvider.getCurrentList();
        while (log != null) {
            result.addAll(0, log.items);

            boolean hasOldItems = false;
            for (Iterator<ChangeItem> it = result.iterator(); it.hasNext(); ) {
                ChangeItem item = it.next();
                if (proxyDatabase.alreadyApplied(item.objectWrapper.type, item.id)) {
                    it.remove();
                    hasOldItems = true;
                } else {
                    break;
                }
            }

            if (!hasOldItems) {
                log = changesListProvider.getPreviousLog(log.previousVersion);
            } else {
                log = null;
            }
        }
        return result;
    }

    void uploadAccounts() throws Exception {
        List<SyncAccount> dirtyAccounts = proxyDatabase.getDirtyAccounts();
        if (dirtyAccounts.isEmpty()) return;

        ChangesListProvider changesListProvider = listProviders.get(SyncConstants.ACCOUNT_TYPE);
        ChangesList list = changesListProvider.getCurrentList();
        uploadDirtyObjects(dirtyAccounts, SyncConstants.ACCOUNT_TYPE, list, "accounts");
    }

    private <T extends SyncObject> void uploadDirtyObjects(List<T> dirtyObjects, int type, ChangesList list, String logSuffix) throws Exception {
        logger.print("Uploading " + logSuffix);
        final List<ChangeItem> newItems = new ArrayList<>();
        for (T obj : dirtyObjects) {
            ChangeItem item;
            if (obj.removed) {
                item = new ChangeItem(
                        list.nextCommandId(),
                        SyncConstants.DELETE_ACTION,
                        new ObjectWrapper(type, obj)
                );

            } else if (obj.globalId >= 0) {
                item = new ChangeItem(
                        list.nextCommandId(),
                        SyncConstants.UPDATE_ACTION,
                        new ObjectWrapper(type, obj)
                );
            } else {
                obj.globalId = obj.preparedGlobalId != null ? obj.preparedGlobalId : list.nextGlobalId();
                item = new ChangeItem(
                        list.nextCommandId(),
                        SyncConstants.CREATE_ACTION,
                        new ObjectWrapper(type, obj)
                );
                item.sourceId = obj.localId;
                item.sourceDeviceId = deviceId;
            }
            newItems.add(item);
            list.items.add(item);
        }

        listProviders.get(type).putLog(list);
        proxyDatabase.runInTx(new Runnable() {
            @Override
            public void run() {
                proxyDatabase.clearDirties(newItems);
            }
        });
    }

    private String generateLockId() {
        return UUID.randomUUID().toString();
    }

    /**
     * For tests purposes
     *
     * @param deviceId deviceId
     */
    void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }
}

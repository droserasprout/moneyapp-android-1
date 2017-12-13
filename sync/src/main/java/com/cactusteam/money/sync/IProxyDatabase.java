package com.cactusteam.money.sync;

import com.cactusteam.money.sync.changes.ChangeItem;
import com.cactusteam.money.sync.changes.ChangesList;
import com.cactusteam.money.sync.model.SyncAccount;
import com.cactusteam.money.sync.model.SyncBudget;
import com.cactusteam.money.sync.model.SyncCategory;
import com.cactusteam.money.sync.model.SyncDebt;
import com.cactusteam.money.sync.model.SyncDebtNote;
import com.cactusteam.money.sync.model.SyncPattern;
import com.cactusteam.money.sync.model.SyncSubcategory;
import com.cactusteam.money.sync.model.SyncTransaction;

import java.util.List;

/**
 * @author vpotapenko
 */
public interface IProxyDatabase {

    void runInTx(Runnable r);

    boolean alreadyApplied(int type, long logItemId);
    void clearDirties(List<ChangeItem> items);

    void createAccount(long commandId, SyncAccount account);
    void updateAccount(long commandId, SyncAccount account);
    void deleteAccount(long commandId, SyncAccount account);
    void mergeAccount(long commandId, SyncAccount account, Long sourceId);
    List<SyncAccount> getDirtyAccounts() throws Exception;
    boolean hasDirtyAccounts();

    void createCategory(long commandId, SyncCategory category);
    void updateCategory(long commandId, SyncCategory category);
    void deleteCategory(long commandId, SyncCategory category);
    void mergeCategory(long commandId, SyncCategory category, Long sourceId);
    List<SyncCategory> getDirtyCategories() throws Exception;
    boolean hasDirtyCategories();

    void createSubcategory(long commandId, SyncSubcategory subcategory);
    void updateSubcategory(long commandId, SyncSubcategory subcategory);
    void deleteSubcategory(long commandId, SyncSubcategory subcategory);
    void mergeSubcategory(long commandId, SyncSubcategory subcategory, Long sourceId);
    List<SyncSubcategory> getDirtySubcategories() throws Exception;
    boolean hasDirtySubcategories();

    void createTransaction(long commandId, SyncTransaction transaction) throws Exception;
    void updateTransaction(long commandId, SyncTransaction transaction) throws Exception;
    void deleteTransaction(long commandId, SyncTransaction transaction);
    void mergeTransaction(long commandId, SyncTransaction transaction, Long sourceId) throws Exception;
    List<SyncTransaction> getDirtyTransactions() throws Exception;
    boolean hasDirtyTransactions();

    void createDebt(long commandId, SyncDebt debt);
    void updateDebt(long commandId, SyncDebt debt);
    void deleteDebt(long commandId, SyncDebt debt);
    void mergeDebt(long commandId, SyncDebt debt, Long sourceId);
    List<SyncDebt> getDirtyDebts() throws Exception;
    boolean hasDirtyDebts();

    void createDebtNote(long commandId, SyncDebtNote debtNote);
    void updateDebtNote(long commandId, SyncDebtNote debtNote);
    void deleteDebtNote(long commandId, SyncDebtNote debtNote);
    void mergeDebtNote(long commandId, SyncDebtNote debtNote, Long sourceId);
    List<SyncDebtNote> getDirtyDebtNotes() throws Exception;
    boolean hasDirtyDebtNotes();

    void createPattern(long commandId, SyncPattern pattern) throws Exception;
    void updatePattern(long commandId, SyncPattern pattern) throws Exception;
    void deletePattern(long commandId, SyncPattern pattern) throws Exception;
    void mergePattern(long commandId, SyncPattern pattern, Long sourceId) throws Exception;
    List<SyncPattern> getDirtyPatterns() throws Exception;
    boolean hasDirtyPatterns();

    void createBudget(long commandId, SyncBudget obj) throws Exception;
    void updateBudget(long commandId, SyncBudget obj) throws Exception;
    void deleteBudget(long commandId, SyncBudget obj) throws Exception;
    void mergeBudget(long commandId, SyncBudget budget, Long sourceId) throws Exception;
    List<SyncBudget> getDirtyBudgets(ChangesList list) throws Exception;
    boolean hasDirtyBudgets();

    boolean isOutdatedLock(String lock, int type);
    void saveLock(String lock, int type);
}

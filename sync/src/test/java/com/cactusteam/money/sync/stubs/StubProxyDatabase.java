package com.cactusteam.money.sync.stubs;

import com.cactusteam.money.sync.IProxyDatabase;
import com.cactusteam.money.sync.changes.ChangesList;
import com.cactusteam.money.sync.model.SyncAccount;
import com.cactusteam.money.sync.model.SyncBudget;
import com.cactusteam.money.sync.model.SyncCategory;
import com.cactusteam.money.sync.model.SyncDebt;
import com.cactusteam.money.sync.model.SyncDebtNote;
import com.cactusteam.money.sync.model.SyncPattern;
import com.cactusteam.money.sync.model.SyncSubcategory;
import com.cactusteam.money.sync.model.SyncTransaction;
import com.cactusteam.money.sync.SyncConstants;
import com.cactusteam.money.sync.changes.ChangeItem;

import java.util.ArrayList;
import java.util.List;

/**
 * @author vpotapenko
 */
public class StubProxyDatabase implements IProxyDatabase {

    public final List<DbCommand> oldCommands = new ArrayList<>();
    public final List<SyncAccount> dirtyAccounts = new ArrayList<>();
    public final List<SyncCategory> dirtyCategories = new ArrayList<>();
    public final List<SyncSubcategory> dirtySubcategories = new ArrayList<>();
    public final List<SyncTransaction> dirtyTransactions = new ArrayList<>();
    public final List<SyncDebt> dirtyDebts = new ArrayList<>();
    public final List<SyncPattern> dirtyPatterns = new ArrayList<>();
    public final List<SyncBudget> dirtyBudgets = new ArrayList<>();

    public List<ChangeItem> committedItems;

    @Override
    public void runInTx(Runnable r) {
        r.run();
    }

    @Override
    public boolean alreadyApplied(int type, long commandId) {
        for (DbCommand c : oldCommands) {
            if (c.type == type && c.id == commandId) return true;
        }
        return false;
    }

    @Override
    public void clearDirties(List<ChangeItem> items) {
        committedItems = items;
    }

    @Override
    public void createAccount(long commandId, SyncAccount account) {
        DbCommand c = new DbCommand();
        c.type = SyncConstants.ACCOUNT_TYPE;
        c.action = SyncConstants.CREATE_ACTION;
        c.id = commandId;
        c.obj = account;
        oldCommands.add(c);
    }

    @Override
    public void updateAccount(long commandId, SyncAccount account) {
        DbCommand c = new DbCommand();
        c.type = SyncConstants.ACCOUNT_TYPE;
        c.action = SyncConstants.UPDATE_ACTION;
        c.id = commandId;
        c.obj = account;
        oldCommands.add(c);
    }

    @Override
    public void deleteAccount(long commandId, SyncAccount account) {
        DbCommand c = new DbCommand();
        c.type = SyncConstants.ACCOUNT_TYPE;
        c.action = SyncConstants.DELETE_ACTION;
        c.id = commandId;
        c.obj = account;
        oldCommands.add(c);
    }

    @Override
    public void mergeAccount(long commandId, SyncAccount account, Long sourceId) {
        DbCommand c = new DbCommand();
        c.type = SyncConstants.ACCOUNT_TYPE;
        c.action = -1;
        c.id = commandId;
        c.obj = account;
        oldCommands.add(c);
    }

    @Override
    public List<SyncAccount> getDirtyAccounts() {
        return dirtyAccounts;
    }

    @Override
    public boolean hasDirtyAccounts() {
        return !dirtyAccounts.isEmpty();
    }

    @Override
    public void createCategory(long commandId, SyncCategory category) {
        DbCommand c = new DbCommand();
        c.type = SyncConstants.CATEGORY_TYPE;
        c.action = SyncConstants.CREATE_ACTION;
        c.id = commandId;
        c.obj = category;
        oldCommands.add(c);
    }

    @Override
    public void updateCategory(long commandId, SyncCategory category) {
        DbCommand c = new DbCommand();
        c.type = SyncConstants.CATEGORY_TYPE;
        c.action = SyncConstants.UPDATE_ACTION;
        c.id = commandId;
        c.obj = category;
        oldCommands.add(c);
    }

    @Override
    public void deleteCategory(long commandId, SyncCategory category) {
        DbCommand c = new DbCommand();
        c.type = SyncConstants.CATEGORY_TYPE;
        c.action = SyncConstants.DELETE_ACTION;
        c.id = commandId;
        c.obj = category;
        oldCommands.add(c);
    }

    @Override
    public void mergeCategory(long commandId, SyncCategory category, Long sourceId) {
        DbCommand c = new DbCommand();
        c.type = SyncConstants.CATEGORY_TYPE;
        c.action = -1;
        c.id = commandId;
        c.obj = category;
        oldCommands.add(c);
    }

    @Override
    public List<SyncCategory> getDirtyCategories() throws Exception {
        return dirtyCategories;
    }

    @Override
    public boolean hasDirtyCategories() {
        return !dirtyCategories.isEmpty();
    }

    @Override
    public void createSubcategory(long commandId, SyncSubcategory subcategory) {
        DbCommand c = new DbCommand();
        c.type = SyncConstants.SUBCATEGORY_TYPE;
        c.action = SyncConstants.CREATE_ACTION;
        c.id = commandId;
        c.obj = subcategory;
        oldCommands.add(c);
    }

    @Override
    public void updateSubcategory(long commandId, SyncSubcategory subcategory) {
        DbCommand c = new DbCommand();
        c.type = SyncConstants.SUBCATEGORY_TYPE;
        c.action = SyncConstants.UPDATE_ACTION;
        c.id = commandId;
        c.obj = subcategory;
        oldCommands.add(c);
    }

    @Override
    public void deleteSubcategory(long commandId, SyncSubcategory subcategory) {
        DbCommand c = new DbCommand();
        c.type = SyncConstants.SUBCATEGORY_TYPE;
        c.action = SyncConstants.DELETE_ACTION;
        c.id = commandId;
        c.obj = subcategory;
        oldCommands.add(c);
    }

    @Override
    public void mergeSubcategory(long commandId, SyncSubcategory subcategory, Long sourceId) {
        DbCommand c = new DbCommand();
        c.type = SyncConstants.SUBCATEGORY_TYPE;
        c.action = -1;
        c.id = commandId;
        c.obj = subcategory;
        oldCommands.add(c);
    }

    @Override
    public List<SyncSubcategory> getDirtySubcategories() throws Exception {
        return dirtySubcategories;
    }

    @Override
    public boolean hasDirtySubcategories() {
        return !dirtySubcategories.isEmpty();
    }

    @Override
    public void createTransaction(long commandId, SyncTransaction transaction) {
        DbCommand c = new DbCommand();
        c.type = SyncConstants.TRANSACTION_TYPE;
        c.action = SyncConstants.CREATE_ACTION;
        c.id = commandId;
        c.obj = transaction;
        oldCommands.add(c);
    }

    @Override
    public void updateTransaction(long commandId, SyncTransaction transaction) {
        DbCommand c = new DbCommand();
        c.type = SyncConstants.TRANSACTION_TYPE;
        c.action = SyncConstants.UPDATE_ACTION;
        c.id = commandId;
        c.obj = transaction;
        oldCommands.add(c);
    }

    @Override
    public void deleteTransaction(long commandId, SyncTransaction transaction) {
        DbCommand c = new DbCommand();
        c.type = SyncConstants.TRANSACTION_TYPE;
        c.action = SyncConstants.DELETE_ACTION;
        c.id = commandId;
        c.obj = transaction;
        oldCommands.add(c);
    }

    @Override
    public void mergeTransaction(long commandId, SyncTransaction transaction, Long sourceId) {
        DbCommand c = new DbCommand();
        c.type = SyncConstants.TRANSACTION_TYPE;
        c.action = -1;
        c.id = commandId;
        c.obj = transaction;
        oldCommands.add(c);
    }

    @Override
    public List<SyncTransaction> getDirtyTransactions() throws Exception {
        return dirtyTransactions;
    }

    @Override
    public boolean hasDirtyTransactions() {
        return !dirtyTransactions.isEmpty();
    }

    @Override
    public void createDebt(long commandId, SyncDebt debt) {
        DbCommand c = new DbCommand();
        c.type = SyncConstants.DEBT_TYPE;
        c.action = SyncConstants.CREATE_ACTION;
        c.id = commandId;
        c.obj = debt;
        oldCommands.add(c);
    }

    @Override
    public void updateDebt(long commandId, SyncDebt debt) {
        DbCommand c = new DbCommand();
        c.type = SyncConstants.DEBT_TYPE;
        c.action = SyncConstants.UPDATE_ACTION;
        c.id = commandId;
        c.obj = debt;
        oldCommands.add(c);
    }

    @Override
    public void deleteDebt(long commandId, SyncDebt debt) {
        DbCommand c = new DbCommand();
        c.type = SyncConstants.DEBT_TYPE;
        c.action = SyncConstants.DELETE_ACTION;
        c.id = commandId;
        c.obj = debt;
        oldCommands.add(c);
    }

    @Override
    public void mergeDebt(long commandId, SyncDebt debt, Long sourceId) {
        DbCommand c = new DbCommand();
        c.type = SyncConstants.DEBT_TYPE;
        c.action = -1;
        c.id = commandId;
        c.obj = debt;
        oldCommands.add(c);
    }

    @Override
    public List<SyncDebt> getDirtyDebts() throws Exception {
        return dirtyDebts;
    }

    @Override
    public boolean hasDirtyDebts() {
        return !dirtyDebts.isEmpty();
    }

    @Override
    public void createDebtNote(long commandId, SyncDebtNote debtNote) {

    }

    @Override
    public void updateDebtNote(long commandId, SyncDebtNote debtNote) {

    }

    @Override
    public void deleteDebtNote(long commandId, SyncDebtNote debtNote) {

    }

    @Override
    public void mergeDebtNote(long commandId, SyncDebtNote debtNote, Long sourceId) {

    }

    @Override
    public List<SyncDebtNote> getDirtyDebtNotes() throws Exception {
        return null;
    }

    @Override
    public boolean hasDirtyDebtNotes() {
        return false;
    }

    @Override
    public void createPattern(long commandId, SyncPattern obj) {
        DbCommand c = new DbCommand();
        c.type = SyncConstants.PATTERN_TYPE;
        c.action = SyncConstants.CREATE_ACTION;
        c.id = commandId;
        c.obj = obj;
        oldCommands.add(c);
    }

    @Override
    public void updatePattern(long commandId, SyncPattern obj) {
        DbCommand c = new DbCommand();
        c.type = SyncConstants.PATTERN_TYPE;
        c.action = SyncConstants.UPDATE_ACTION;
        c.id = commandId;
        c.obj = obj;
        oldCommands.add(c);
    }

    @Override
    public void deletePattern(long commandId, SyncPattern obj) {
        DbCommand c = new DbCommand();
        c.type = SyncConstants.PATTERN_TYPE;
        c.action = SyncConstants.DELETE_ACTION;
        c.id = commandId;
        c.obj = obj;
        oldCommands.add(c);
    }

    @Override
    public void mergePattern(long commandId, SyncPattern pattern, Long sourceId) {
        DbCommand c = new DbCommand();
        c.type = SyncConstants.PATTERN_TYPE;
        c.action = -1;
        c.id = commandId;
        c.obj = pattern;
        oldCommands.add(c);
    }

    @Override
    public List<SyncPattern> getDirtyPatterns() throws Exception {
        return dirtyPatterns;
    }

    @Override
    public boolean hasDirtyPatterns() {
        return !dirtyPatterns.isEmpty();
    }

    @Override
    public void createBudget(long commandId, SyncBudget obj) {
        DbCommand c = new DbCommand();
        c.type = SyncConstants.BUDGET_TYPE;
        c.action = SyncConstants.CREATE_ACTION;
        c.id = commandId;
        c.obj = obj;
        oldCommands.add(c);
    }

    @Override
    public void updateBudget(long commandId, SyncBudget obj) {
        DbCommand c = new DbCommand();
        c.type = SyncConstants.BUDGET_TYPE;
        c.action = SyncConstants.UPDATE_ACTION;
        c.id = commandId;
        c.obj = obj;
        oldCommands.add(c);
    }

    @Override
    public void deleteBudget(long commandId, SyncBudget obj) {
        DbCommand c = new DbCommand();
        c.type = SyncConstants.BUDGET_TYPE;
        c.action = SyncConstants.DELETE_ACTION;
        c.id = commandId;
        c.obj = obj;
        oldCommands.add(c);
    }

    @Override
    public void mergeBudget(long commandId, SyncBudget budget, Long sourceId) {
        DbCommand c = new DbCommand();
        c.type = SyncConstants.BUDGET_TYPE;
        c.action = -1;
        c.id = commandId;
        c.obj = budget;
        oldCommands.add(c);
    }

    @Override
    public List<SyncBudget> getDirtyBudgets(ChangesList list) throws Exception {
        return dirtyBudgets;
    }

    @Override
    public boolean hasDirtyBudgets() {
        return !dirtyBudgets.isEmpty();
    }

    @Override
    public boolean isOutdatedLock(String lock, int type) {
        return true;
    }

    @Override
    public void saveLock(String lock, int type) {
    }

    public static class DbCommand {
        public int type;
        public int action;
        public long id;
        public Object obj;
    }
}

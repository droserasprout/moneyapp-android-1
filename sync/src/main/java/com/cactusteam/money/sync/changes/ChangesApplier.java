package com.cactusteam.money.sync.changes;

import com.cactusteam.money.sync.IProxyDatabase;
import com.cactusteam.money.sync.SyncConstants;
import com.cactusteam.money.sync.SyncException;
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
public class ChangesApplier {

    private final String deviceId;

    public ChangesApplier(String deviceId) {
        this.deviceId = deviceId;
    }

    public void execute(final IProxyDatabase proxy, final List<ChangeItem> items) throws Exception {
        proxy.runInTx(new Runnable() {
            @Override
            public void run() {
                applyItems(proxy, items);
            }
        });
    }

    private void applyItems(IProxyDatabase proxy, List<ChangeItem> items) {
        for (ChangeItem item : items) {
            try {
                apply(proxy, item);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void apply(IProxyDatabase proxy, ChangeItem item) throws Exception {
        int type = item.objectWrapper.type;
        if (proxy.alreadyApplied(type, item.id)) return; // item already applied before

        if (type == SyncConstants.ACCOUNT_TYPE) {
            applyAccount(proxy, item);
        } else if (type == SyncConstants.CATEGORY_TYPE) {
            applyCategory(proxy, item);
        } else if (type == SyncConstants.SUBCATEGORY_TYPE) {
            applySubcategory(proxy, item);
        } else if (type == SyncConstants.TRANSACTION_TYPE) {
            applyTransaction(proxy, item);
        } else if (type == SyncConstants.DEBT_TYPE) {
            applyDebt(proxy, item);
        } else if (type == SyncConstants.DEBT_NOTE_TYPE) {
            applyDebtNote(proxy, item);
        } else if (type == SyncConstants.PATTERN_TYPE) {
            applyPattern(proxy, item);
        } else if (type == SyncConstants.BUDGET_TYPE) {
            applyBudget(proxy, item);
        } else {
            throw new SyncException("Wrong type in log item " + item.id);
        }


    }

    private void applyBudget(IProxyDatabase proxy, ChangeItem item) throws Exception {
        if (item.action == SyncConstants.CREATE_ACTION) {
            if (deviceId.equals(item.sourceDeviceId)) {
                proxy.mergeBudget(item.id, (SyncBudget) item.objectWrapper.obj, item.sourceId);
            } else {
                proxy.createBudget(item.id, (SyncBudget) item.objectWrapper.obj);
            }
        } else if (item.action == SyncConstants.UPDATE_ACTION) {
            proxy.updateBudget(item.id, (SyncBudget) item.objectWrapper.obj);
        } else if (item.action == SyncConstants.DELETE_ACTION) {
            proxy.deleteBudget(item.id, (SyncBudget) item.objectWrapper.obj);
        } else {
            throw new SyncException("Wrong action in log item  " + item.id);
        }
    }

    private void applyPattern(IProxyDatabase proxy, ChangeItem item) throws Exception {
        if (item.action == SyncConstants.CREATE_ACTION) {
            if (deviceId.equals(item.sourceDeviceId)) {
                proxy.mergePattern(item.id, (SyncPattern) item.objectWrapper.obj, item.sourceId);
            } else {
                proxy.createPattern(item.id, (SyncPattern) item.objectWrapper.obj);
            }
        } else if (item.action == SyncConstants.UPDATE_ACTION) {
            proxy.updatePattern(item.id, (SyncPattern) item.objectWrapper.obj);
        } else if (item.action == SyncConstants.DELETE_ACTION) {
            proxy.deletePattern(item.id, (SyncPattern) item.objectWrapper.obj);
        } else {
            throw new SyncException("Wrong action in log item  " + item.id);
        }
    }

    private void applyDebt(IProxyDatabase proxy, ChangeItem item) throws Exception {
        if (item.action == SyncConstants.CREATE_ACTION) {
            if (deviceId.equals(item.sourceDeviceId)) {
                proxy.mergeDebt(item.id, (SyncDebt) item.objectWrapper.obj, item.sourceId);
            } else {
                proxy.createDebt(item.id, (SyncDebt) item.objectWrapper.obj);
            }
        } else if (item.action == SyncConstants.UPDATE_ACTION) {
            proxy.updateDebt(item.id, (SyncDebt) item.objectWrapper.obj);
        } else if (item.action == SyncConstants.DELETE_ACTION) {
            proxy.deleteDebt(item.id, (SyncDebt) item.objectWrapper.obj);
        } else {
            throw new SyncException("Wrong action in log item  " + item.id);
        }
    }

    private void applyDebtNote(IProxyDatabase proxy, ChangeItem item) throws Exception {
        if (item.action == SyncConstants.CREATE_ACTION) {
            if (deviceId.equals(item.sourceDeviceId)) {
                proxy.mergeDebtNote(item.id, (SyncDebtNote) item.objectWrapper.obj, item.sourceId);
            } else {
                proxy.createDebtNote(item.id, (SyncDebtNote) item.objectWrapper.obj);
            }
        } else if (item.action == SyncConstants.UPDATE_ACTION) {
            proxy.updateDebtNote(item.id, (SyncDebtNote) item.objectWrapper.obj);
        } else if (item.action == SyncConstants.DELETE_ACTION) {
            proxy.deleteDebtNote(item.id, (SyncDebtNote) item.objectWrapper.obj);
        } else {
            throw new SyncException("Wrong action in log item  " + item.id);
        }
    }

    private void applyTransaction(IProxyDatabase proxy, ChangeItem item) throws Exception {
        if (item.action == SyncConstants.CREATE_ACTION) {
            if (deviceId.equals(item.sourceDeviceId)) {
                proxy.mergeTransaction(item.id, (SyncTransaction) item.objectWrapper.obj, item.sourceId);
            } else {
                proxy.createTransaction(item.id, (SyncTransaction) item.objectWrapper.obj);
            }
        } else if (item.action == SyncConstants.UPDATE_ACTION) {
            proxy.updateTransaction(item.id, (SyncTransaction) item.objectWrapper.obj);
        } else if (item.action == SyncConstants.DELETE_ACTION) {
            proxy.deleteTransaction(item.id, (SyncTransaction) item.objectWrapper.obj);
        } else {
            throw new SyncException("Wrong action in log item  " + item.id);
        }
    }

    private void applySubcategory(IProxyDatabase proxy, ChangeItem item) throws SyncException {
        if (item.action == SyncConstants.CREATE_ACTION) {
            if (deviceId.equals(item.sourceDeviceId)) {
                proxy.mergeSubcategory(item.id, (SyncSubcategory) item.objectWrapper.obj, item.sourceId);
            } else {
                proxy.createSubcategory(item.id, (SyncSubcategory) item.objectWrapper.obj);
            }
        } else if (item.action == SyncConstants.UPDATE_ACTION) {
            proxy.updateSubcategory(item.id, (SyncSubcategory) item.objectWrapper.obj);
        } else if (item.action == SyncConstants.DELETE_ACTION) {
            proxy.deleteSubcategory(item.id, (SyncSubcategory) item.objectWrapper.obj);
        } else {
            throw new SyncException("Wrong action in log item  " + item.id);
        }
    }

    private void applyCategory(IProxyDatabase proxy, ChangeItem item) throws Exception {
        if (item.action == SyncConstants.CREATE_ACTION) {
            if (deviceId.equals(item.sourceDeviceId)) {
                proxy.mergeCategory(item.id, (SyncCategory) item.objectWrapper.obj, item.sourceId);
            } else {
                proxy.createCategory(item.id, (SyncCategory) item.objectWrapper.obj);
            }
        } else if (item.action == SyncConstants.UPDATE_ACTION) {
            proxy.updateCategory(item.id, (SyncCategory) item.objectWrapper.obj);
        } else if (item.action == SyncConstants.DELETE_ACTION) {
            proxy.deleteCategory(item.id, (SyncCategory) item.objectWrapper.obj);
        } else {
            throw new SyncException("Wrong action in log item  " + item.id);
        }
    }

    private void applyAccount(IProxyDatabase proxy, ChangeItem item) throws Exception {
        if (item.action == SyncConstants.CREATE_ACTION) {
            if (deviceId.equals(item.sourceDeviceId)) {
                proxy.mergeAccount(item.id, (SyncAccount) item.objectWrapper.obj, item.sourceId);
            } else {
                proxy.createAccount(item.id, (SyncAccount) item.objectWrapper.obj);
            }
        } else if (item.action == SyncConstants.UPDATE_ACTION) {
            proxy.updateAccount(item.id, (SyncAccount) item.objectWrapper.obj);
        } else if (item.action == SyncConstants.DELETE_ACTION) {
            proxy.deleteAccount(item.id, (SyncAccount) item.objectWrapper.obj);
        } else {
            throw new SyncException("Wrong action in log item  " + item.id);
        }
    }
}

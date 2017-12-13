package com.cactusteam.money.data;

import com.cactusteam.money.data.dao.Account;
import com.cactusteam.money.data.dao.BudgetPlan;
import com.cactusteam.money.data.dao.BudgetPlanDependency;
import com.cactusteam.money.data.dao.Category;
import com.cactusteam.money.data.dao.CurrencyRate;
import com.cactusteam.money.data.dao.Debt;
import com.cactusteam.money.data.dao.DebtNote;
import com.cactusteam.money.data.dao.PatternTag;
import com.cactusteam.money.data.dao.Subcategory;
import com.cactusteam.money.data.dao.SyncLog;
import com.cactusteam.money.data.dao.Transaction;
import com.cactusteam.money.data.dao.TransactionPattern;
import com.cactusteam.money.data.dao.TransactionTag;
import com.cactusteam.money.data.dao.Trash;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

/**
 * @author vpotapenko
 */
public class JsonConverter {

    public JSONObject toJson(Account account) throws JSONException {
        JSONObject object = new JSONObject();

        object.put(DataConstants.ID, account.getId());
        object.put(DataConstants.NAME, account.getName());
        object.put(DataConstants.DELETED, account.getDeleted());
        object.put(DataConstants.CURRENCY_CODE, account.getCurrencyCode());
        object.put(DataConstants.COLOR, account.getColor());
        object.put(DataConstants.TYPE, account.getType());
        object.put(DataConstants.SKIP_IN_BALANCE, account.getSkipInBalance());
        object.put(DataConstants.CUSTOM_ORDER, account.getCustomOrder());
        object.put(DataConstants.SYNCED, account.getSynced());
        object.put(DataConstants.GLOBAL_ID, account.getGlobalId());

        return object;
    }

    public Account createAccount(JSONObject object) {
        Account account = new Account();

        account.setId(object.optLong(DataConstants.ID));
        account.setName(object.optString(DataConstants.NAME));
        account.setDeleted(object.optBoolean(DataConstants.DELETED));
        account.setCurrencyCode(object.optString(DataConstants.CURRENCY_CODE));
        account.setColor(object.optString(DataConstants.COLOR));
        account.setType(object.optInt(DataConstants.TYPE, Account.CASH_TYPE));
        account.setSkipInBalance(object.optBoolean(DataConstants.SKIP_IN_BALANCE, false));
        account.setCustomOrder(object.optInt(DataConstants.CUSTOM_ORDER, 0));
        account.setSynced(extractBoolean(object, DataConstants.SYNCED));
        account.setGlobalId(extractLong(object, DataConstants.GLOBAL_ID));

        return account;
    }

    public JSONObject toJson(BudgetPlan plan) throws JSONException {
        JSONObject object = new JSONObject();

        object.put(DataConstants.NAME, plan.getName());
        object.put(DataConstants.START, plan.getStart().getTime());
        object.put(DataConstants.FINISH, plan.getFinish().getTime());
        object.put(DataConstants.LIMIT, plan.getLimit());
        object.put(DataConstants.TYPE, plan.getType());
        object.put(DataConstants.NEXT, plan.getNext());
        object.put(DataConstants.SYNCED, plan.getSynced());
        object.put(DataConstants.GLOBAL_ID, plan.getGlobalId());

        JSONArray array = new JSONArray();
        for (BudgetPlanDependency dependency : plan.getDependencies()) {
            array.put(toJson(dependency));
        }
        object.put(DataConstants.DEPENDENCIES, array);


        return object;
    }

    public BudgetPlan createPlan(JSONObject object) {
        BudgetPlan plan = new BudgetPlan();

        plan.setName(object.optString(DataConstants.NAME));
        plan.setStart(new Date(object.optLong(DataConstants.START, System.currentTimeMillis())));
        plan.setFinish(new Date(object.optLong(DataConstants.FINISH, System.currentTimeMillis())));
        plan.setLimit(object.optDouble(DataConstants.LIMIT));
        plan.setType(object.optInt(DataConstants.TYPE, BudgetPlan.ONE_TIME_TYPE));
        plan.setNext(extractLong(object, DataConstants.NEXT));
        plan.setSynced(extractBoolean(object, DataConstants.SYNCED));
        plan.setGlobalId(extractLong(object, DataConstants.GLOBAL_ID));

        return plan;
    }

    public JSONObject toJson(BudgetPlanDependency dependency) throws JSONException {
        JSONObject object = new JSONObject();

        object.put(DataConstants.REF_TYPE, dependency.getRefType());
        object.put(DataConstants.REF, dependency.getRefId());

        return object;
    }

    public BudgetPlanDependency createPlanDependency(JSONObject object) {
        BudgetPlanDependency dependency = new BudgetPlanDependency();

        dependency.setRefType(object.optInt(DataConstants.REF_TYPE));
        dependency.setRefId(object.optString(DataConstants.REF));

        return dependency;
    }

    public JSONObject toJson(Category category) throws JSONException {
        JSONObject object = new JSONObject();

        object.put(DataConstants.ID, category.getId());
        object.put(DataConstants.TYPE, category.getType());
        object.put(DataConstants.NAME, category.getName());
        object.put(DataConstants.ICON, category.getIcon());
        object.put(DataConstants.DELETED, category.getDeleted());
        object.put(DataConstants.SYNCED, category.getSynced());
        object.put(DataConstants.GLOBAL_ID, category.getGlobalId());

        JSONArray array = new JSONArray();
        for (Subcategory subcategory : category.getSubcategories()) {
            array.put(toJson(subcategory));
        }
        object.put(DataConstants.SUBCATEGORIES, array);

        return object;
    }

    public Category createCategory(JSONObject object) {
        Category category = new Category();

        category.setId(object.optLong(DataConstants.ID));
        category.setType(object.optInt(DataConstants.TYPE));
        category.setName(object.optString(DataConstants.NAME));
        category.setIcon(object.optString(DataConstants.ICON));
        category.setDeleted(object.optBoolean(DataConstants.DELETED));
        category.setSynced(extractBoolean(object, DataConstants.SYNCED));
        category.setGlobalId(extractLong(object, DataConstants.GLOBAL_ID));

        return category;
    }

    public JSONObject toJson(Subcategory subcategory) throws JSONException {
        JSONObject object = new JSONObject();

        object.put(DataConstants.ID, subcategory.getId());
        object.put(DataConstants.NAME, subcategory.getName());
        object.put(DataConstants.DELETED, subcategory.getDeleted());
        object.put(DataConstants.SYNCED, subcategory.getSynced());
        object.put(DataConstants.GLOBAL_ID, subcategory.getGlobalId());

        return object;
    }

    public Subcategory createSubcategory(JSONObject object) {
        Subcategory subcategory = new Subcategory();

        subcategory.setId(object.optLong(DataConstants.ID));
        subcategory.setName(object.optString(DataConstants.NAME));
        subcategory.setDeleted(object.optBoolean(DataConstants.DELETED));
        subcategory.setSynced(extractBoolean(object, DataConstants.SYNCED));
        subcategory.setGlobalId(extractLong(object, DataConstants.GLOBAL_ID));

        return subcategory;
    }

    public JSONObject toJson(CurrencyRate rate) throws JSONException {
        JSONObject object = new JSONObject();

        object.put(DataConstants.DATE, rate.getDate().getTime());
        object.put(DataConstants.SOURCE, rate.getSourceCurrencyCode());
        object.put(DataConstants.DEST, rate.getDestCurrencyCode());
        object.put(DataConstants.RATE, rate.getRate());

        return object;
    }

    public CurrencyRate createRate(JSONObject object) {
        CurrencyRate rate = new CurrencyRate();

        rate.setDate(new Date(object.optLong(DataConstants.DATE, System.currentTimeMillis())));
        rate.setSourceCurrencyCode(object.optString(DataConstants.SOURCE));
        rate.setDestCurrencyCode(object.optString(DataConstants.DEST));
        rate.setRate(object.optDouble(DataConstants.RATE));

        return rate;
    }

    public JSONObject toJson(Debt debt) throws JSONException {
        JSONObject object = new JSONObject();

        object.put(DataConstants.ID, debt.getId());
        object.put(DataConstants.NAME, debt.getName());
        object.put(DataConstants.PHONE, debt.getPhone());
        object.put(DataConstants.CONTACT, debt.getContactId());
        object.put(DataConstants.DATE, debt.getTill().getTime());
        object.put(DataConstants.ACCOUNT, debt.getAccountId());
        object.put(DataConstants.FINISHED, debt.getFinished());
        if (debt.getStart() != null) object.put(DataConstants.START, debt.getStart().getTime());
        object.put(DataConstants.SYNCED, debt.getSynced());
        object.put(DataConstants.GLOBAL_ID, debt.getGlobalId());

        return object;
    }

    public Debt createDebt(JSONObject object) {
        Debt debt = new Debt();

        debt.setId(object.optLong(DataConstants.ID));
        debt.setName(object.optString(DataConstants.NAME));
        debt.setPhone(object.optString(DataConstants.PHONE));
        debt.setContactId(object.optLong(DataConstants.CONTACT));
        debt.setTill(new Date(object.optLong(DataConstants.DATE, System.currentTimeMillis())));

        Long value = extractLong(object, DataConstants.START);
        if (value != null) debt.setStart(new Date(value));

        debt.setAccountId(object.optLong(DataConstants.ACCOUNT));
        debt.setFinished(object.optBoolean(DataConstants.FINISHED, false));
        debt.setSynced(extractBoolean(object, DataConstants.SYNCED));
        debt.setGlobalId(extractLong(object, DataConstants.GLOBAL_ID));

        return debt;
    }

    public JSONObject toJson(DebtNote debtNote) throws JSONException {
        JSONObject object = new JSONObject();

        object.put(DataConstants.ID, debtNote.getId());
        object.put(DataConstants.DEBT_ID, debtNote.getDebtId());
        object.put(DataConstants.TEXT, debtNote.getText());
        object.put(DataConstants.DATE, debtNote.getDate());
        object.put(DataConstants.SYNCED, debtNote.getSynced());
        object.put(DataConstants.GLOBAL_ID, debtNote.getGlobalId());

        return object;
    }

    public DebtNote createDebtNote(JSONObject object) {
        DebtNote debtNote = new DebtNote();

        debtNote.setId(object.optLong(DataConstants.ID));
        debtNote.setDebtId(object.optLong(DataConstants.DEBT_ID));
        debtNote.setText(object.optString(DataConstants.TEXT));
        debtNote.setDate(new Date(object.optLong(DataConstants.DATE, System.currentTimeMillis())));

        debtNote.setSynced(extractBoolean(object, DataConstants.SYNCED));
        debtNote.setGlobalId(extractLong(object, DataConstants.GLOBAL_ID));

        return debtNote;
    }

    public JSONObject toJson(Transaction transaction) throws JSONException {
        JSONObject object = new JSONObject();

        object.put(DataConstants.TYPE, transaction.getType());
        object.put(DataConstants.DATE, transaction.getDate().getTime());
        object.put(DataConstants.COMMENT, transaction.getComment());
        object.put(DataConstants.REF, transaction.getRef());
        object.put(DataConstants.STATUS, transaction.getStatus());
        object.put(DataConstants.ACCOUNT, transaction.getSourceAccountId());
        object.put(DataConstants.AMOUNT, transaction.getAmount());
        object.put(DataConstants.CATEGORY, transaction.getCategoryId());
        object.put(DataConstants.SUBCATEGORY, transaction.getSubcategoryId());
        object.put(DataConstants.DEST_ACCOUNT, transaction.getDestAccountId());
        object.put(DataConstants.DEST_AMOUNT, transaction.getDestAmount());
        object.put(DataConstants.SYNCED, transaction.getSynced());
        object.put(DataConstants.GLOBAL_ID, transaction.getGlobalId());

        JSONArray array = new JSONArray();
        for (TransactionTag tag : transaction.getTags()) {
            array.put(tag.getTag().getName());
        }
        object.put(DataConstants.TAGS, array);

        return object;
    }

    public Transaction createTransaction(JSONObject object) {
        Transaction transaction = new Transaction();

        transaction.setType(object.optInt(DataConstants.TYPE));
        transaction.setDate(new Date(object.optLong(DataConstants.DATE, System.currentTimeMillis())));
        transaction.setComment(object.optString(DataConstants.COMMENT, null));
        transaction.setRef(object.optString(DataConstants.REF));
        transaction.setStatus(object.optInt(DataConstants.STATUS, Transaction.STATUS_COMPLETED));
        transaction.setSourceAccountId(object.optLong(DataConstants.ACCOUNT));
        transaction.setAmount(object.optDouble(DataConstants.AMOUNT));
        transaction.setCategoryId(extractLong(object, DataConstants.CATEGORY));
        transaction.setSubcategoryId(extractLong(object, DataConstants.SUBCATEGORY));
        transaction.setDestAccountId(extractLong(object, DataConstants.DEST_ACCOUNT));
        transaction.setDestAmount(extractDouble(object, DataConstants.DEST_AMOUNT));
        transaction.setSynced(extractBoolean(object, DataConstants.SYNCED));
        transaction.setGlobalId(extractLong(object, DataConstants.GLOBAL_ID));

        return transaction;
    }

    public Long extractLong(JSONObject object, String key) {
        return object.has(key) ? object.optLong(key) : null;
    }

    public Double extractDouble(JSONObject object, String key) {
        return object.has(key) ? object.optDouble(key) : null;
    }

    public Boolean extractBoolean(JSONObject object, String key) {
        return object.has(key) ? object.optBoolean(key) : null;
    }

    public JSONObject toJson(TransactionPattern pattern) throws JSONException {
        JSONObject object = new JSONObject();

        object.put(DataConstants.NAME, pattern.getName());
        object.put(DataConstants.TYPE, pattern.getType());
        object.put(DataConstants.COMMENT, pattern.getComment());
        object.put(DataConstants.ACCOUNT, pattern.getSourceAccountId());
        object.put(DataConstants.AMOUNT, pattern.getAmount());
        object.put(DataConstants.CATEGORY, pattern.getCategoryId());
        object.put(DataConstants.SUBCATEGORY, pattern.getSubcategoryId());
        object.put(DataConstants.DEST_ACCOUNT, pattern.getDestAccountId());
        object.put(DataConstants.DEST_AMOUNT, pattern.getDestAmount());
        object.put(DataConstants.SYNCED, pattern.getSynced());
        object.put(DataConstants.GLOBAL_ID, pattern.getGlobalId());

        JSONArray array = new JSONArray();
        for (PatternTag tag : pattern.getTags()) {
            array.put(tag.getTag().getName());
        }
        object.put(DataConstants.TAGS, array);

        return object;
    }

    public TransactionPattern createPattern(JSONObject object) {
        TransactionPattern pattern = new TransactionPattern();

        pattern.setName(object.optString(DataConstants.NAME));
        pattern.setType(object.optInt(DataConstants.TYPE));
        pattern.setComment(object.optString(DataConstants.COMMENT, null));
        pattern.setSourceAccountId(object.optLong(DataConstants.ACCOUNT));
        pattern.setAmount(object.optDouble(DataConstants.AMOUNT));
        pattern.setCategoryId(extractLong(object, DataConstants.CATEGORY));
        pattern.setSubcategoryId(extractLong(object, DataConstants.SUBCATEGORY));
        pattern.setDestAccountId(extractLong(object, DataConstants.DEST_ACCOUNT));
        pattern.setDestAmount(extractDouble(object, DataConstants.DEST_AMOUNT));
        pattern.setSynced(extractBoolean(object, DataConstants.SYNCED));
        pattern.setGlobalId(extractLong(object, DataConstants.GLOBAL_ID));

        return pattern;
    }

    public JSONObject toJson(SyncLog syncLog) throws JSONException {
        JSONObject object = new JSONObject();

        object.put(DataConstants.TYPE, syncLog.getType());
        object.put(DataConstants.GLOBAL_ID, syncLog.getGlobalId());

        return object;
    }

    public SyncLog createSyncLog(JSONObject object) {
        SyncLog syncLog = new SyncLog();

        syncLog.setType(object.optInt(DataConstants.TYPE));
        syncLog.setGlobalId(object.optLong(DataConstants.GLOBAL_ID));

        return syncLog;
    }

    public JSONObject toJson(Trash trash) throws JSONException {
        JSONObject object = new JSONObject();

        object.put(DataConstants.TYPE, trash.getType());
        object.put(DataConstants.GLOBAL_ID, trash.getGlobalId());

        return object;
    }

    public Trash createTrash(JSONObject object) {
        Trash trash = new Trash();

        trash.setType(object.optInt(DataConstants.TYPE));
        trash.setGlobalId(object.optLong(DataConstants.GLOBAL_ID));

        return trash;
    }
}

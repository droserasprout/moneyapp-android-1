package com.cactusteam.dao;

import de.greenrobot.daogenerator.DaoGenerator;
import de.greenrobot.daogenerator.Entity;
import de.greenrobot.daogenerator.Property;
import de.greenrobot.daogenerator.Schema;
import de.greenrobot.daogenerator.ToMany;
import de.greenrobot.daogenerator.ToOne;

/**
 * @author vpotapenko
 */
public class MyDaoGenerator {

    private static final String APP_SRC_PATH = "./app/src/main/java";

    public static void main(String[] args) throws Exception {
        new MyDaoGenerator().generateAll();
    }

    private void generateAll() throws Exception {
        Schema schema = new Schema(14, "com.cactusteam.money.data.dao");
        schema.enableKeepSectionsByDefault();

        prepareSchema(schema);

        new DaoGenerator().generateAll(schema, APP_SRC_PATH);
    }

    private void prepareSchema(Schema schema) {
        Entity account = schema.addEntity("Account");
        account.implementsInterface("ISyncObject");
        account.addIdProperty();
        account.addStringProperty("name").notNull();
        account.addBooleanProperty("deleted").notNull();
        account.addStringProperty("currencyCode").notNull();
        account.addStringProperty("color").notNull();
        account.addIntProperty("type").notNull();
        account.addBooleanProperty("skipInBalance").notNull();
        account.addIntProperty("customOrder").notNull();
        account.addLongProperty("globalId");
        account.addBooleanProperty("synced");

        Entity category = schema.addEntity("Category");
        category.implementsInterface("ISyncObject");
        category.addIdProperty();
        category.addIntProperty("type").notNull();
        category.addStringProperty("name").notNull();
        category.addStringProperty("icon");
        category.addBooleanProperty("deleted").notNull();
        category.addIntProperty("customOrder").notNull();
        category.addLongProperty("globalId");
        category.addBooleanProperty("synced");

        Entity subcategory = schema.addEntity("Subcategory");
        subcategory.implementsInterface("ISyncObject");
        subcategory.addIdProperty();
        Property nameProperty = subcategory.addStringProperty("name").notNull().getProperty();
        subcategory.addBooleanProperty("deleted").notNull();
        Property categoryId = subcategory.addLongProperty("categoryId").notNull().getProperty();
        subcategory.addToOne(category, categoryId);
        subcategory.addLongProperty("globalId");
        subcategory.addBooleanProperty("synced");

        ToMany toMany = category.addToMany(subcategory, categoryId);
        toMany.setName("subcategories");
        toMany.orderAsc(nameProperty);

        Entity transaction = schema.addEntity("Transaction");
        transaction.implementsInterface("ISyncObject");
        transaction.setTableName("OPERATION");
        transaction.addIdProperty();
        transaction.addIntProperty("type").notNull();
        transaction.addDateProperty("date").notNull();
        transaction.addStringProperty("comment");
        transaction.addStringProperty("ref");
        transaction.addLongProperty("globalId");
        transaction.addBooleanProperty("synced");
        transaction.addIntProperty("status").notNull();

        Property sourceAccountId = transaction.addLongProperty("sourceAccountId").notNull().getProperty();
        ToOne toOne = transaction.addToOne(account, sourceAccountId);
        toOne.setName("sourceAccount");

        transaction.addDoubleProperty("amount").notNull();

        categoryId = transaction.addLongProperty("categoryId").getProperty();
        transaction.addToOne(category, categoryId).setName("category");

        Property subcategoryId = transaction.addLongProperty("subcategoryId").getProperty();
        transaction.addToOne(subcategory, subcategoryId).setName("subcategory");

        Property destAccountId = transaction.addLongProperty("destAccountId").getProperty();
        toOne = transaction.addToOne(account, destAccountId);
        toOne.setName("destAccount");
        transaction.addDoubleProperty("destAmount");

        Entity tag = schema.addEntity("Tag");
        tag.addIdProperty();
        tag.addStringProperty("name").notNull();
        tag.addDateProperty("updated").notNull();

        Entity transactionTag = schema.addEntity("TransactionTag");
        transactionTag.implementsInterface("ITagContainer");
        transactionTag.addIdProperty();

        Property transactionId = transactionTag.addLongProperty("transactionId").notNull().getProperty();
        Property tagId = transactionTag.addLongProperty("tagId").notNull().getProperty();
        transactionTag.addToOne(tag, tagId);

        transaction.addToMany(transactionTag, transactionId, "tags");

        Entity currencyRate = schema.addEntity("CurrencyRate");
        currencyRate.implementsInterface("android.os.Parcelable");
        currencyRate.addIdProperty();
        currencyRate.addDateProperty("date").notNull();
        currencyRate.addStringProperty("sourceCurrencyCode").notNull();
        currencyRate.addStringProperty("destCurrencyCode").notNull();
        currencyRate.addDoubleProperty("rate").notNull();

        Entity budgetPlan = schema.addEntity("BudgetPlan");
        budgetPlan.implementsInterface("ISyncObject");
        budgetPlan.addIdProperty();
        budgetPlan.addStringProperty("name").notNull();
        budgetPlan.addDateProperty("start").notNull();
        budgetPlan.addDateProperty("finish").notNull();
        budgetPlan.addDoubleProperty("limit").notNull();
        budgetPlan.addIntProperty("type").notNull();
        budgetPlan.addLongProperty("next");
        budgetPlan.addLongProperty("globalId");
        budgetPlan.addBooleanProperty("synced");

        Entity budgetPlanDependency = schema.addEntity("BudgetPlanDependency");
        budgetPlanDependency.addIdProperty();
        budgetPlanDependency.addIntProperty("refType").notNull();
        budgetPlanDependency.addStringProperty("refId").notNull();
        Property planId = budgetPlanDependency.addLongProperty("planId").notNull().getProperty();

        budgetPlan.addToMany(budgetPlanDependency, planId, "dependencies");

        Entity debt = schema.addEntity("Debt");
        debt.implementsInterface("ISyncObject");
        debt.addIdProperty();
        debt.addStringProperty("name").notNull();
        debt.addStringProperty("phone");
        debt.addLongProperty("contactId");
        debt.addDateProperty("till").notNull();
        debt.addDateProperty("start");
        debt.addBooleanProperty("finished").notNull();
        debt.addLongProperty("globalId");
        debt.addBooleanProperty("synced");

        Property accountProperty = debt.addLongProperty("accountId").notNull().getProperty();
        debt.addToOne(account, accountProperty);

        Entity debtNote = schema.addEntity("DebtNote");
        debtNote.implementsInterface("ISyncObject");
        debtNote.addIdProperty();
        Property debtId = debtNote.addLongProperty("debtId").notNull().getProperty();
        debtNote.addDateProperty("date").notNull();
        debtNote.addStringProperty("text");
        debtNote.addLongProperty("globalId");
        debtNote.addBooleanProperty("synced");

        toMany = debt.addToMany(debtNote, debtId);
        toMany.setName("notes");

        Entity transactionPattern = schema.addEntity("TransactionPattern");
        transactionPattern.implementsInterface("ISyncObject");
        transactionPattern.addIdProperty();
        transactionPattern.addStringProperty("name");
        transactionPattern.addIntProperty("type").notNull();
        transactionPattern.addStringProperty("comment");
        transactionPattern.addLongProperty("globalId");
        transactionPattern.addBooleanProperty("synced");

        sourceAccountId = transactionPattern.addLongProperty("sourceAccountId").notNull().getProperty();
        toOne = transactionPattern.addToOne(account, sourceAccountId);
        toOne.setName("sourceAccount");

        transactionPattern.addDoubleProperty("amount").notNull();

        categoryId = transactionPattern.addLongProperty("categoryId").getProperty();
        transactionPattern.addToOne(category, categoryId).setName("category");

        subcategoryId = transactionPattern.addLongProperty("subcategoryId").getProperty();
        transactionPattern.addToOne(subcategory, subcategoryId).setName("subcategory");

        destAccountId = transactionPattern.addLongProperty("destAccountId").getProperty();
        toOne = transactionPattern.addToOne(account, destAccountId);
        toOne.setName("destAccount");
        transactionPattern.addDoubleProperty("destAmount");

        Entity patternTag = schema.addEntity("PatternTag");
        patternTag.implementsInterface("ITagContainer");
        patternTag.addIdProperty();

        Property patternId = patternTag.addLongProperty("patternId").notNull().getProperty();
        tagId = patternTag.addLongProperty("tagId").notNull().getProperty();
        patternTag.addToOne(tag, tagId);

        transactionPattern.addToMany(patternTag, patternId, "tags");

        Entity note = schema.addEntity("Note");
        note.addIdProperty();
        note.addStringProperty("ref");
        note.addStringProperty("description").notNull();

        Entity syncLog = schema.addEntity("SyncLog");
        syncLog.addIdProperty();
        syncLog.addLongProperty("globalId").notNull();
        syncLog.addIntProperty("type").notNull();

        Entity trash = schema.addEntity("Trash");
        trash.addIdProperty();
        trash.addIntProperty("type").notNull();
        trash.addLongProperty("globalId").notNull();

        Entity syncLock = schema.addEntity("SyncLock");
        syncLock.addIdProperty();
        syncLock.addIntProperty("type").notNull();
        syncLock.addStringProperty("lockId").notNull();
        syncLock.addLongProperty("time").notNull();
    }
}

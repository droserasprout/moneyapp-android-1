package com.cactusteam.money.sync.changes;

import com.cactusteam.money.sync.SyncConstants;
import com.cactusteam.money.sync.model.SyncAccount;
import com.cactusteam.money.sync.model.SyncBudget;
import com.cactusteam.money.sync.model.SyncCategory;
import com.cactusteam.money.sync.model.SyncDebt;
import com.cactusteam.money.sync.model.SyncDebtNote;
import com.cactusteam.money.sync.model.SyncObject;
import com.cactusteam.money.sync.model.SyncPattern;
import com.cactusteam.money.sync.model.SyncSubcategory;
import com.cactusteam.money.sync.model.SyncTransaction;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;

/**
 * @author vpotapenko
 */
class ObjectWrapperDeserializer extends JsonDeserializer<ObjectWrapper> {

    @Override
    public ObjectWrapper deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        ObjectWrapper objectWrapper = new ObjectWrapper();

        JsonNode node = p.getCodec().readTree(p);
        objectWrapper.type = node.get("type").asInt();
        if (objectWrapper.type == SyncConstants.ACCOUNT_TYPE) {
            JsonNode jsonNode = node.get("obj");
            objectWrapper.obj = extractAccount(jsonNode);
        } else if (objectWrapper.type == SyncConstants.CATEGORY_TYPE) {
            JsonNode jsonNode = node.get("obj");
            objectWrapper.obj = extractCategory(jsonNode);
        } else if (objectWrapper.type == SyncConstants.SUBCATEGORY_TYPE) {
            JsonNode jsonNode = node.get("obj");
            objectWrapper.obj = extractSubcategory(jsonNode);
        } else if (objectWrapper.type == SyncConstants.TRANSACTION_TYPE) {
            JsonNode jsonNode = node.get("obj");
            objectWrapper.obj = extractTransaction(jsonNode);
        } else if (objectWrapper.type == SyncConstants.DEBT_TYPE) {
            JsonNode jsonNode = node.get("obj");
            objectWrapper.obj = extractDebt(jsonNode);
        } else if (objectWrapper.type == SyncConstants.DEBT_NOTE_TYPE) {
            JsonNode jsonNode = node.get("obj");
            objectWrapper.obj = extractDebtNote(jsonNode);
        } else if (objectWrapper.type == SyncConstants.PATTERN_TYPE) {
            JsonNode jsonNode = node.get("obj");
            objectWrapper.obj = extractPattern(jsonNode);
        } else if (objectWrapper.type == SyncConstants.BUDGET_TYPE) {
            JsonNode jsonNode = node.get("obj");
            objectWrapper.obj = extractBudget(jsonNode);
        }

        return objectWrapper;
    }

    private SyncBudget extractBudget(JsonNode jsonNode) {
        SyncBudget budget = new SyncBudget();

        budget.globalId = jsonNode.get("globalId").asLong();
        budget.start = jsonNode.get("start").asLong();
        budget.finish = jsonNode.get("finish").asLong();
        budget.limit = jsonNode.get("limit").asDouble();
        budget.type = jsonNode.get("type").asInt();

        budget.name = jsonNode.hasNonNull("name") ? jsonNode.get("name").asText() : null;
        budget.nextGlobalId = jsonNode.hasNonNull("nextGlobalId") ? jsonNode.get("nextGlobalId").asLong() : null;

        JsonNode dependencies = jsonNode.get("dependencies");
        for (JsonNode d : dependencies) {
            int type = d.get("type").asInt();
            String refGlobalId = d.hasNonNull("refGlobalId") ? d.get("refGlobalId").asText() : null;
            budget.dependencies.add(new SyncBudget.Dependency(type, refGlobalId));
        }

        return budget;
    }

    private SyncPattern extractPattern(JsonNode jsonNode) {
        SyncPattern pattern = new SyncPattern();

        pattern.globalId = jsonNode.get("globalId").asLong();
        pattern.type = jsonNode.get("type").asInt();
        pattern.globalSourceAccountId = jsonNode.get("globalSourceAccountId").asLong();
        pattern.amount = jsonNode.get("amount").asDouble();

        pattern.name = jsonNode.hasNonNull("name") ? jsonNode.get("name").asText() : null;
        pattern.comment = jsonNode.hasNonNull("comment") ? jsonNode.get("comment").asText() : null;
        pattern.globalCategoryId = jsonNode.hasNonNull("globalCategoryId") ? jsonNode.get("globalCategoryId").asLong() : null;
        pattern.globalSubcategoryId = jsonNode.hasNonNull("globalSubcategoryId") ? jsonNode.get("globalSubcategoryId").asLong() : null;
        pattern.globalDestAccountId = jsonNode.hasNonNull("globalDestAccountId") ? jsonNode.get("globalDestAccountId").asLong() : null;
        pattern.destAmount = jsonNode.hasNonNull("destAmount") ? jsonNode.get("destAmount").asDouble() : null;

        JsonNode tags = jsonNode.get("tags");
        for (JsonNode n : tags) {
            pattern.tags.add(n.asText());
        }

        return pattern;
    }

    private SyncObject extractDebt(JsonNode jsonNode) {
        SyncDebt debt = new SyncDebt();

        debt.globalId = jsonNode.get("globalId").asLong();
        debt.name = jsonNode.hasNonNull("name") ? jsonNode.get("name").asText() : null;
        debt.globalAccountId = jsonNode.get("globalAccountId").asLong();

        debt.phone = jsonNode.hasNonNull("phone") ? jsonNode.get("phone").asText() : null;
        debt.finished = jsonNode.hasNonNull("finished") && jsonNode.get("finished").asBoolean();
        debt.till = jsonNode.hasNonNull("till") ? jsonNode.get("till").asLong() : null;
        debt.start = jsonNode.hasNonNull("start") ? jsonNode.get("start").asLong() : null;

        return debt;
    }

    private SyncObject extractDebtNote(JsonNode jsonNode) {
        SyncDebtNote debtNote = new SyncDebtNote();

        debtNote.globalId = jsonNode.get("globalId").asLong();
        debtNote.globalDebtId = jsonNode.get("globalDebtId").asLong();
        debtNote.text = jsonNode.hasNonNull("text") ? jsonNode.get("text").asText() : null;
        debtNote.date = jsonNode.hasNonNull("date") ? jsonNode.get("date").asLong() : null;

        return debtNote;
    }

    private SyncTransaction extractTransaction(JsonNode jsonNode) {
        SyncTransaction transaction = new SyncTransaction();

        transaction.globalId = jsonNode.get("globalId").asLong();
        transaction.type = jsonNode.get("type").asInt();
        transaction.date = jsonNode.get("date").asLong();
        transaction.globalSourceAccountId = jsonNode.get("globalSourceAccountId").asLong();
        transaction.amount = jsonNode.get("amount").asDouble();

        transaction.comment = jsonNode.hasNonNull("comment") ? jsonNode.get("comment").asText() : null;
        transaction.ref = jsonNode.hasNonNull("ref") ? jsonNode.get("ref").asText() : null;
        transaction.status = jsonNode.hasNonNull("status") ? jsonNode.get("status").asInt() : 0;
        transaction.globalCategoryId = jsonNode.hasNonNull("globalCategoryId") ? jsonNode.get("globalCategoryId").asLong() : null;
        transaction.globalSubcategoryId = jsonNode.hasNonNull("globalSubcategoryId") ? jsonNode.get("globalSubcategoryId").asLong() : null;
        transaction.globalDestAccountId = jsonNode.hasNonNull("globalDestAccountId") ? jsonNode.get("globalDestAccountId").asLong() : null;
        transaction.destAmount = jsonNode.hasNonNull("destAmount") ? jsonNode.get("destAmount").asDouble() : null;

        JsonNode tags = jsonNode.get("tags");
        for (JsonNode n : tags) {
            transaction.tags.add(n.asText());
        }

        return transaction;
    }

    private SyncSubcategory extractSubcategory(JsonNode jsonNode) {
        SyncSubcategory subcategory = new SyncSubcategory();

        subcategory.globalId = jsonNode.get("globalId").asLong();
        subcategory.globalCategoryId = jsonNode.get("globalCategoryId").asLong();
        subcategory.deleted = jsonNode.get("deleted").asBoolean();

        subcategory.name = jsonNode.hasNonNull("name") ? jsonNode.get("name").asText() : null;

        return subcategory;
    }

    private SyncCategory extractCategory(JsonNode jsonNode) {
        SyncCategory category = new SyncCategory();

        category.globalId = jsonNode.get("globalId").asLong();
        category.type = jsonNode.get("type").asInt();
        category.deleted = jsonNode.get("deleted").asBoolean();

        category.name = jsonNode.hasNonNull("name") ? jsonNode.get("name").asText() : null;
        category.icon = jsonNode.hasNonNull("icon") ? jsonNode.get("icon").asText() : null;

        return category;
    }

    private SyncAccount extractAccount(JsonNode jsonNode) {
        SyncAccount account = new SyncAccount();

        account.globalId = jsonNode.get("globalId").asLong();
        account.type = jsonNode.get("type").asInt();
        account.deleted = jsonNode.get("deleted").asBoolean();

        account.name = jsonNode.hasNonNull("name") ? jsonNode.get("name").asText() : null;
        account.currencyCode = jsonNode.hasNonNull("currencyCode") ? jsonNode.get("currencyCode").asText() : null;

        account.color = jsonNode.hasNonNull("color") ? jsonNode.get("color").asText() : null;
        account.skipInBalance = jsonNode.hasNonNull("skipInBalance") && jsonNode.get("skipInBalance").asBoolean();

        return account;
    }
}

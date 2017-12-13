package com.cactusteam.money.data;

/**
 * @author vpotapenko
 */
public class DataConstants {

    public static final int DEFAULT_COPY_BUFFER_SIZE = 1024;
    public static final int DEFAULT_BACKUP_NUMBERS = 10;

    // types
    public static final int CATEGORY_TYPE = 0;
    public static final int SUBCATEGORY_TYPE = 1;
    public static final int TAG_TYPE = 2;
    public static final int ACCOUNT_TYPE = 3;
    public static final int BUDGET_TYPE = 4;
    public static final int CATEGORY_NAME_TYPE = 5;

    // backup
    public static final String ID = "id";
    public static final String NAME = "name";
    public static final String DELETED = "deleted";
    public static final String CURRENCY_CODE = "currencyCode";
    public static final String COLOR = "color";
    public static final String START = "start";
    public static final String FINISH = "finish";
    public static final String LIMIT = "limit";
    public static final String DEPENDENCIES = "dependencies";
    public static final String REF_TYPE = "refType";
    public static final String REF = "ref";
    public static final String STATUS = "status";
    public static final String TYPE = "type";
    public static final String SKIP_IN_BALANCE = "skipInBalance";
    public static final String CUSTOM_ORDER = "customOrder";
    public static final String ICON = "icon";
    public static final String SUBCATEGORIES = "subcategories";
    public static final String SOURCE = "source";
    public static final String DEST = "dest";
    public static final String DATE = "date";
    public static final String RATE = "rate";
    public static final String PHONE = "phone";
    public static final String TEXT = "text";
    public static final String DEBT_ID = "debtId";
    public static final String CONTACT = "contact";
    public static final String ACCOUNT = "account";
    public static final String COMMENT = "comment";
    public static final String AMOUNT = "amount";
    public static final String CATEGORY = "category";
    public static final String SUBCATEGORY = "subcategory";
    public static final String DEST_ACCOUNT = "destAccount";
    public static final String DEST_AMOUNT = "destAmount";
    public static final String TAGS = "tags";
    public static final String ACCOUNTS = "accounts";
    public static final String BUDGET = "budget";
    public static final String CATEGORIES = "categories";
    public static final String RATES = "rates";
    public static final String DEBTS = "debts";
    public static final String DEBT_NOTES = "debtNotes";
    public static final String TRANSACTIONS = "transactions";
    public static final String PATTERNS = "patterns";
    public static final String SYNC_LOGS = "syncLogs";
    public static final String TRASH = "trash";
    public static final String PERIOD = "period";
    public static final String PREFS = "pr";
    public static final String SYNC_TYPE = "syncType";
    public static final String SYNC_TOKEN = "syncToken";
    public static final String NEXT = "next";
    public static final String FINISHED = "finished";
    public static final String SYNCED = "synced";
    public static final String GLOBAL_ID = "globalId";

    // files
    public static final String BACKUP_FILENAME_PREFIX = "moneyApp_";
    public static final String BACKUP_FILENAME_SUFFIX = ".mappb";
    public static final String AUTO_BACKUP_FILENAME_SUFFIX = ".mappba";
    public static final String TRANSACTIONS_EXPORT_PREFIX = "transactions_";
}

package com.cactusteam.money.data.dao;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

/**
 * @author vpotapenko
 */
public class DbOpenHelper extends DaoMaster.OpenHelper {

    public DbOpenHelper(Context context, String name, SQLiteDatabase.CursorFactory factory) {
        super(context, name, factory);
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        for (int i = oldVersion; i < newVersion; i++) {
            Patch patch = PATCHES[i];
            if (patch != null) patch.apply(db);
        }
    }

    private interface Patch {

        void apply(SQLiteDatabase db);
    }

    private static final Patch[] PATCHES = new Patch[]{
            /** Version 1 no patch */
            null,
            /** Version 2 patch */
            new Patch() {
                @Override
                public void apply(SQLiteDatabase db) {
                    db.execSQL("ALTER TABLE '" + BudgetPlanDao.TABLENAME + "' ADD COLUMN 'TYPE' INTEGER NOT NULL DEFAULT 0");
                    db.execSQL("ALTER TABLE '" + BudgetPlanDao.TABLENAME + "' ADD COLUMN 'NEXT' INTEGER");
                }
            },
            /** Version 3 patch */
            new Patch() {
                @Override
                public void apply(SQLiteDatabase db) {
                    db.execSQL("ALTER TABLE '" + AccountDao.TABLENAME + "' ADD COLUMN 'TYPE' INTEGER NOT NULL DEFAULT 0");
                }
            },
            /** Version 4 patch */
            new Patch() {
                @Override
                public void apply(SQLiteDatabase db) {
                    db.execSQL("ALTER TABLE '" + AccountDao.TABLENAME + "' ADD COLUMN 'GLOBAL_ID' INTEGER");
                    db.execSQL("ALTER TABLE '" + AccountDao.TABLENAME + "' ADD COLUMN 'SYNCED' INTEGER");

                    db.execSQL("ALTER TABLE '" + BudgetPlanDao.TABLENAME + "' ADD COLUMN 'GLOBAL_ID' INTEGER");
                    db.execSQL("ALTER TABLE '" + BudgetPlanDao.TABLENAME + "' ADD COLUMN 'SYNCED' INTEGER");

                    db.execSQL("ALTER TABLE '" + CategoryDao.TABLENAME + "' ADD COLUMN 'GLOBAL_ID' INTEGER");
                    db.execSQL("ALTER TABLE '" + CategoryDao.TABLENAME + "' ADD COLUMN 'SYNCED' INTEGER");

                    db.execSQL("ALTER TABLE '" + DebtDao.TABLENAME + "' ADD COLUMN 'GLOBAL_ID' INTEGER");
                    db.execSQL("ALTER TABLE '" + DebtDao.TABLENAME + "' ADD COLUMN 'SYNCED' INTEGER");

                    db.execSQL("ALTER TABLE '" + SubcategoryDao.TABLENAME + "' ADD COLUMN 'GLOBAL_ID' INTEGER");
                    db.execSQL("ALTER TABLE '" + SubcategoryDao.TABLENAME + "' ADD COLUMN 'SYNCED' INTEGER");

                    db.execSQL("ALTER TABLE '" + TransactionDao.TABLENAME + "' ADD COLUMN 'GLOBAL_ID' INTEGER");
                    db.execSQL("ALTER TABLE '" + TransactionDao.TABLENAME + "' ADD COLUMN 'SYNCED' INTEGER");

                    db.execSQL("ALTER TABLE '" + TransactionPatternDao.TABLENAME + "' ADD COLUMN 'GLOBAL_ID' INTEGER");
                    db.execSQL("ALTER TABLE '" + TransactionPatternDao.TABLENAME + "' ADD COLUMN 'SYNCED' INTEGER");

                    SyncLogDao.createTable(db, true);
                }
            },
            /** Version 5 patch */
            new Patch() {
                @Override
                public void apply(SQLiteDatabase db) {
                    TrashDao.createTable(db, true);
                }
            },
            /** Version 6 patch */
            new Patch() {
                @Override
                public void apply(SQLiteDatabase db) {
                    SyncLockDao.createTable(db, true);
                }
            },
            /** Version 7 patch */
            new Patch() {
                @Override
                public void apply(SQLiteDatabase db) {
                    db.execSQL("ALTER TABLE '" + AccountDao.TABLENAME + "' ADD COLUMN 'SKIP_IN_BALANCE' INTEGER NOT NULL DEFAULT 0");
                    db.execSQL("UPDATE " + AccountDao.TABLENAME + " SET SKIP_IN_BALANCE=1 WHERE TYPE=1");
                }
            },
            /** Version 8 patch */
            new Patch() {
                @Override
                public void apply(SQLiteDatabase db) {
                    db.execSQL("ALTER TABLE '" + AccountDao.TABLENAME + "' ADD COLUMN 'CUSTOM_ORDER' INTEGER NOT NULL DEFAULT 0");
                }
            },
            /** Version 9 patch */
            new Patch() {
                @Override
                public void apply(SQLiteDatabase db) {
                    db.execSQL("ALTER TABLE '" + CategoryDao.TABLENAME + "' ADD COLUMN 'CUSTOM_ORDER' INTEGER NOT NULL DEFAULT 0");
                }
            },
            /** Version 10 patch */
            new Patch() {
                @Override
                public void apply(SQLiteDatabase db) {
                    db.execSQL("DELETE FROM " + CurrencyRateDao.TABLENAME + " WHERE " + CurrencyRateDao.Properties.SourceCurrencyCode.columnName + "='BYN' AND " + CurrencyRateDao.Properties.DestCurrencyCode.columnName + "='BYR'");
                    db.execSQL("DELETE FROM " + CurrencyRateDao.TABLENAME + " WHERE " + CurrencyRateDao.Properties.SourceCurrencyCode.columnName + "='BYR' AND " + CurrencyRateDao.Properties.DestCurrencyCode.columnName + "='BYN'");
                }
            },
            /** Version 11 patch */
            new Patch() {
                @Override
                public void apply(SQLiteDatabase db) {
                    db.execSQL("ALTER TABLE '" + TransactionDao.TABLENAME + "' ADD COLUMN 'STATUS' INTEGER NOT NULL DEFAULT 0");
                    db.execSQL("UPDATE " + TransactionDao.TABLENAME + " SET STATUS=1 WHERE REF='planning'");
                }
            },
            /** Version 12 patch */
            new Patch() {
                @Override
                public void apply(SQLiteDatabase db) {
                    db.execSQL("ALTER TABLE '" + DebtDao.TABLENAME + "' ADD COLUMN 'FINISHED' INTEGER NOT NULL DEFAULT 0");
                }
            },
            /** Version 13 patch */
            new Patch() {
                @Override
                public void apply(SQLiteDatabase db) {
                    db.execSQL("ALTER TABLE '" + DebtDao.TABLENAME + "' ADD COLUMN 'START' INTEGER");
                }
            },
            /** Version 14 patch */
            new Patch() {
                @Override
                public void apply(SQLiteDatabase db) {
                    DebtNoteDao.createTable(db, true);
                }
            }
    };
}

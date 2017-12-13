package com.cactusteam.money.data.io

import com.cactusteam.money.data.DataManager
import com.cactusteam.money.data.dao.AccountDao
import com.cactusteam.money.data.dao.Category
import com.cactusteam.money.data.dao.CategoryDao
import com.cactusteam.money.data.dao.SubcategoryDao

/**
 * @author vpotapenko
 */
abstract class BaseImporter : IImporter {

    protected var dataManager: DataManager? = null

    var currentSchema: ImportSchema? = null
    protected var importResult: ImportResult? = null

    var previousTransferPart: TransferPart? = null

    fun analyseAccount(accountName: String, currencyCode: String?) {
        var importAccount: ImportAccount? = currentSchema!!.getAccount(accountName, currencyCode)
        if (importAccount != null) return

        val result = dataManager!!.daoSession.accountDao.queryBuilder()
                .where(AccountDao.Properties.Name.eq(accountName),
                        AccountDao.Properties.CurrencyCode.eq(currencyCode),
                        AccountDao.Properties.Deleted.eq(false)).limit(1).list()
        if (!result.isEmpty()) {
            val account = result[0]
            importAccount = ImportAccount(accountName, currencyCode, ExistAccountImportStrategy(account))
        } else {
            importAccount = ImportAccount(accountName, currencyCode, NewAccountImportStrategy(accountName))
        }
        currentSchema!!.putAccount(accountName, currencyCode, importAccount)
    }

    fun analyseCategory(categoryName: String, expense: Boolean) {
        var importCategory: ImportCategory? = currentSchema!!.getCategory(categoryName, expense)
        if (importCategory != null) return

        val result = dataManager!!.daoSession.categoryDao.queryBuilder()
                .where(CategoryDao.Properties.Name.eq(categoryName),
                        CategoryDao.Properties.Type.eq(if (expense) Category.EXPENSE else Category.INCOME),
                        CategoryDao.Properties.Deleted.eq(false)).limit(1).list()
        if (!result.isEmpty()) {
            val category = result[0]
            importCategory = ImportCategory(if (expense) Category.EXPENSE else Category.INCOME, categoryName, ExistCategoryImportStrategy(category))
        } else {
            importCategory = ImportCategory(if (expense) Category.EXPENSE else Category.INCOME, categoryName, NewCategoryImportStrategy(categoryName))
        }
        currentSchema!!.putCategory(categoryName, expense, importCategory)
    }

    fun analyseSubcategory(categoryName: String, subcategoryName: String, expense: Boolean) {
        val importCategory = currentSchema!!.getCategory(categoryName, expense)

        var importSubcategory: ImportSubcategory? = importCategory!!.subcategoryMap[subcategoryName]
        if (importSubcategory != null) return

        val categoryId = importCategory.predictCategoryId()
        if (categoryId == null) {
            importSubcategory = ImportSubcategory(importCategory, subcategoryName, NewSubcategoryImportStrategy())
        } else {
            val result = dataManager!!.daoSession.subcategoryDao.queryBuilder()
                    .where(SubcategoryDao.Properties.CategoryId.eq(categoryId),
                            SubcategoryDao.Properties.Name.eq(subcategoryName),
                            SubcategoryDao.Properties.Deleted.eq(false)).limit(1).list()
            if (result.isEmpty()) {
                importSubcategory = ImportSubcategory(importCategory, subcategoryName, NewSubcategoryImportStrategy())
            } else {
                importSubcategory = ImportSubcategory(importCategory, subcategoryName, ExistSubcategoryImportStrategy(result[0]))
            }
        }
        importCategory.subcategoryMap.put(subcategoryName, importSubcategory)
    }

    fun handleImportException(e: Exception, count: Int) {
        e.printStackTrace()
        log(e.message, count + 1)
    }

    protected fun log(message: String?, line: Int) {
        importResult!!.log.add(ImportResult.ImportLogItem(message, line))
    }

    protected fun getSubcategoryId(categoryName: String, subcategory: String, expense: Boolean): Long? {
        val importCategory = currentSchema!!.getCategory(categoryName, expense)
        val importSubcategory = importCategory!!.subcategoryMap[subcategory]
        return importSubcategory?.getSubcategoryId()
    }

    protected fun getCategoryId(category: String, expense: Boolean): Long? {
        val importCategory = currentSchema!!.getCategory(category, expense)
        return importCategory!!.getCategoryId()
    }

    protected fun getAccountId(accountName: String, currencyCode: String?): Long? {
        val importAccount = currentSchema!!.getAccount(accountName, currencyCode)
        return importAccount!!.accountId
    }

    class TransferPart {

        var accountName: String? = null
        var currencyCode: String? = null
        var amount: String? = null
        var direction: String? = null

        var count: Int = 0

    }
}

package com.cactusteam.money.data.service

import android.support.v4.util.ArrayMap
import android.text.format.DateUtils
import android.util.Pair
import com.cactusteam.money.app.MoneyApp
import com.cactusteam.money.data.model.CategoryAmounts
import com.cactusteam.money.data.model.CategoryPeriodData
import com.cactusteam.money.data.DataConstants
import com.cactusteam.money.data.DataManager
import com.cactusteam.money.data.dao.*
import com.cactusteam.money.data.filter.OrTransactionFilters
import com.cactusteam.money.data.filter.TypeTransactionFilter
import com.cactusteam.money.sync.SyncConstants
import java.util.*

/**
 * @author vpotapenko
 */
abstract class CategoryInternalService(dataManager: DataManager) : BaseService(dataManager) {

    fun getCategoryAmountsInternal(): CategoryAmounts {
        val current = getApplication().period.current

        val filter = OrTransactionFilters()
        filter.addFilter(TypeTransactionFilter(Transaction.EXPENSE))
        filter.addFilter(TypeTransactionFilter(Transaction.INCOME))
        val transactions = dataManager.transactionService
                .newListTransactionsBuilder()
                .putFrom(current.first)
                .putTo(current.second)
                .putConvertToMain(true)
                .putTransactionFilter(filter)
                .listInternal()

        val amounts = CategoryAmounts()
        for (transaction in transactions) {
            amounts.putAmount(transaction.categoryId, transaction.subcategoryId, transaction.amountInMainCurrency)
        }

        return amounts
    }

    fun getSubcategoryInternal(subcategoryId: Long): Subcategory {
        return dataManager.daoSession.subcategoryDao.load(subcategoryId)
    }

    fun deleteSubcategoryInternal(subcategoryId: Long) {
        val daoSession = dataManager.daoSession

        val subcategory = daoSession.subcategoryDao.load(subcategoryId)
        if (hasDependencies(subcategory)) {
            subcategory.deleted = true
            if (subcategory.globalId != null) subcategory.synced = false
            daoSession.update(subcategory)
        } else {
            val category = subcategory.category
            daoSession.delete(subcategory)

            category.resetSubcategories()
            category.subcategories
        }
    }

    fun restoreSubcategoryInternal(subcategoryId: Long): Subcategory {
        val daoSession = dataManager.daoSession

        val subcategory = daoSession.subcategoryDao.load(subcategoryId)
        subcategory.deleted = false
        if (subcategory.globalId != null) subcategory.synced = false

        daoSession.update(subcategory)

        return subcategory
    }

    fun convertToCategoryInternal(subcategoryId: Long): Category {
        val daoSession = dataManager.daoSession
        val subcategory = daoSession.subcategoryDao.load(subcategoryId)

        val newCategory = createCategoryInternal(subcategory.category.type, subcategory.name, null)

        daoSession.runInTx({
            convertSubcategroyTransactionsToCategory(subcategoryId, newCategory)
            convertSubcategoryPatternsToCategory(subcategoryId, newCategory)
            convertSubcategoryBudgetToCategory(subcategoryId, newCategory)
        })

        val category = subcategory.category
        if (subcategory.globalId != null) {
            val trash = Trash()
            trash.globalId = subcategory.globalId
            trash.type = SyncConstants.SUBCATEGORY_TYPE
            daoSession.insert(trash)
        }
        daoSession.delete(subcategory)

        category.resetSubcategories()
        category.subcategories

        return newCategory
    }

    fun updateCategoriesOrderInternal(orders: Map<Long, Int>) {
        val daoSession = dataManager.daoSession
        daoSession.runInTx({
            val queryBuilder = daoSession.categoryDao.queryBuilder()
            for (category in queryBuilder.list()) {
                val order = orders[category.id]
                if (order != null) {
                    category.customOrder = order
                    daoSession.update(category)
                }
            }
        })
    }

    fun updateSubcategoryInternal(id: Long,
                                  name: String,
                                  globalId: Long? = null,
                                  synced: Boolean? = null): Subcategory {
        val daoSession = dataManager.daoSession

        val subcategory = daoSession.subcategoryDao.load(id)
        subcategory.name = name
        if (globalId != null) {
            subcategory.globalId = globalId
            subcategory.synced = synced
        } else if (subcategory.globalId != null) {
            subcategory.synced = false
        }
        daoSession.update(subcategory)

        return subcategory
    }

    fun createSubcategoryInternal(parentId: Long,
                                  name: String,
                                  globalId: Long? = null,
                                  synced: Boolean? = null): Subcategory {
        val daoSession = dataManager.daoSession

        val category = daoSession.categoryDao.load(parentId)

        val subcategory = Subcategory()
        subcategory.categoryId = parentId
        subcategory.name = name
        if (globalId != null) {
            subcategory.globalId = globalId
            subcategory.synced = synced
        }

        daoSession.insert(subcategory)

        category.resetSubcategories()
        category.subcategories

        return subcategory
    }

    fun convertToSubcategoryInternal(id: Long, parentId: Long): Subcategory {
        val daoSession = dataManager.daoSession
        val category = daoSession.categoryDao.load(id)

        val newSubcategory = createSubcategoryInternal(parentId, category.name)

        daoSession.runInTx({
            convertCategoryTransactionsToSubcategory(id, newSubcategory, daoSession)
            convertCategoryPatternsToSubcategory(id, newSubcategory, daoSession)
            convertCategoryBudgetToSubcategory(id, newSubcategory, daoSession)
        })

        for (subcategory in category.subcategories) {
            if (subcategory.globalId != null) {
                val trash = Trash()
                trash.globalId = subcategory.globalId!!
                trash.type = SyncConstants.SUBCATEGORY_TYPE
                daoSession.insert(trash)
            }
            daoSession.delete(subcategory)
        }
        if (category.globalId != null) {
            val trash = Trash()
            trash.globalId = category.globalId!!
            trash.type = SyncConstants.CATEGORY_TYPE
            daoSession.insert(trash)
        }
        daoSession.delete(category)

        return newSubcategory
    }

    fun createCategoryInternal(type: Int,
                               name: String,
                               icon: String?,
                               globalId: Long? = null,
                               synced: Boolean? = null): Category {
        val category = Category()
        category.name = name
        category.type = type
        category.icon = icon
        if (globalId != null) {
            category.globalId = globalId
            category.synced = synced
        }

        dataManager.daoSession.insert(category)

        return category
    }

    fun updateCategoryInternal(id: Long,
                               name: String,
                               icon: String?,
                               globalId: Long? = null,
                               synced: Boolean? = null): Category {
        val daoSession = dataManager.daoSession

        val category = daoSession.categoryDao.load(id)
        category.name = name
        category.icon = icon
        if (globalId != null) {
            category.globalId = globalId
            category.synced = synced
        } else if (category.globalId != null) {
            category.synced = false
        }

        daoSession.update(category)

        return category
    }

    fun deleteCategoryInternal(id: Long) {
        val daoSession = dataManager.daoSession
        val category = daoSession.categoryDao.load(id)

        if (hasDependencies(category)) {
            category.deleted = true
            if (category.globalId != null) category.synced = false
            daoSession.update(category)
        } else {
            for (s in category.subcategories) {
                daoSession.delete(s)
            }
            daoSession.delete(category)
        }
    }

    fun restoreCategoryInternal(id: Long): Category {
        val daoSession = dataManager.daoSession
        val category = daoSession.categoryDao.load(id)
        category.deleted = false
        if (category.globalId != null) category.synced = false

        daoSession.update(category)

        return category
    }

    fun getCategoryInternal(id: Long): Category {
        val category = dataManager.daoSession.categoryDao.load(id)
        category.subcategories

        return category
    }

    fun getCategoryPeriodsDataInternal(id: Long, size: Int = 3, addAll: Boolean = false): List<CategoryPeriodData> {
        val result = ArrayList<CategoryPeriodData>()
        val period = getApplication().period

        var datePair = period.current
        for (i in 0..size - 1) {
            val periodData = loadCategoryData(id, datePair)

            if (addAll
                    || i == 0 // first must be added always
                    || periodData.amount > 0) {
                result.add(periodData)
            }

            datePair = period.getPrevious(datePair)
        }

        return result
    }

    fun getCategoriesInternal(type: Int, includeDeleted: Boolean = false): List<Category> {
        val daoSession = dataManager.daoSession

        val queryBuilder = daoSession.categoryDao.queryBuilder().where(CategoryDao.Properties.Type.eq(type))

        val appPreferences = MoneyApp.instance.appPreferences
        val sortType = if (type == Category.EXPENSE) appPreferences.expenseSortType else appPreferences.incomeSortType

        if (sortType == Category.NAME_SORT) {
            queryBuilder.orderAsc(CategoryDao.Properties.Name)
        } else if (sortType == Category.CUSTOM_SORT) {
            queryBuilder.orderAsc(CategoryDao.Properties.CustomOrder)
        }

        if (!includeDeleted) queryBuilder.where(CategoryDao.Properties.Deleted.eq(false))

        var categories = queryBuilder.list()
        if (sortType == Category.FREQUENCY_SORT) {
            categories = sortByFrequency(categories, type)
        }

        return categories
    }

    // Returns null if result is not predictable
    fun getFrequentlyCategoriesInternal(type: Int): List<Category>? {
        val daoSession = dataManager.daoSession

        val queryBuilder = daoSession.categoryDao.queryBuilder().where(CategoryDao.Properties.Type.eq(type))
        queryBuilder.where(CategoryDao.Properties.Deleted.eq(false))
        val list = queryBuilder.list()

        val groups = mutableMapOf<Long, MutableList<Category>>()
        list.forEach { c ->
            val weight = calculateCategoryWeight(c)

            var g = groups[weight]
            if (g == null) {
                g = mutableListOf()
                groups[weight] = g
            }
            g.add(c)
        }
        if (groups.isEmpty()) return null

        val result = mutableListOf<Category>()
        while (result.size < MAX_FREQUENTLY_ITEMS && !groups.isEmpty()) {
            val e = groups.maxBy { it.key }
            if (e != null) {
                result.addAll(e.value)
                groups.remove(e.key)
            } else {
                break
            }
        }
        if (result.size != list.size) {
            val appPreferences = MoneyApp.instance.appPreferences
            val sortType = if (type == Category.EXPENSE) appPreferences.expenseSortType else appPreferences.incomeSortType

            when (sortType) {
                Category.NAME_SORT -> {
                    result.sortBy { it.name }
                }
                Category.CUSTOM_SORT -> {
                    result.sortBy { it.customOrder }
                }
                Category.FREQUENCY_SORT -> {
                    // do nothing because it is already sorted by previous code
                }
            }

            return result
        } else {
            return null
        }
    }

    private fun calculateCategoryWeight(c: Category): Long {
        return dataManager.transactionService
                .newListTransactionsBuilder()
                .putFrom(Date(System.currentTimeMillis() - (DateUtils.DAY_IN_MILLIS * 30)))
                .putCategoryId(c.id)
                .countInternal()
    }

    private fun convertSubcategoryBudgetToCategory(subcategoryId: Long, newCategory: Category) {
        val daoSession = dataManager.daoSession
        val dependencies = daoSession.budgetPlanDependencyDao.queryBuilder()
                .where(BudgetPlanDependencyDao.Properties.RefType.eq(DataConstants.SUBCATEGORY_TYPE),
                        BudgetPlanDependencyDao.Properties.RefId.eq(subcategoryId.toString()))
                .list()

        for (d in dependencies) {
            d.refId = newCategory.id.toString()
            d.refType = DataConstants.CATEGORY_TYPE
            daoSession.update(d)

            val plan = daoSession.budgetPlanDao.load(d.planId)
            if (plan.globalId != null) {
                plan.synced = false
                daoSession.update(plan)
            }
        }
    }

    private fun convertSubcategoryPatternsToCategory(subcategoryId: Long, newCategory: Category) {
        val daoSession = dataManager.daoSession
        val patterns = daoSession.transactionPatternDao.queryBuilder()
                .where(TransactionPatternDao.Properties.SubcategoryId.eq(subcategoryId))
                .list()
        for (p in patterns) {
            p.categoryId = newCategory.id
            p.subcategoryId = null
            if (p.globalId != null) {
                p.synced = false
            }
            daoSession.update(p)
        }
    }

    private fun convertSubcategroyTransactionsToCategory(subcategoryId: Long, newCategory: Category) {
        val daoSession = dataManager.daoSession
        val transactions = daoSession.transactionDao.queryBuilder()
                .where(TransactionDao.Properties.SubcategoryId.eq(subcategoryId))
                .list()

        for (t in transactions) {
            t.categoryId = newCategory.id
            t.subcategoryId = null
            if (t.globalId != null) {
                t.synced = false
            }
            daoSession.update(t)
        }

    }

    private fun sortByFrequency(categories: List<Category>, type: Int): List<Category> {
        val transactions = dataManager.transactionService
                .newListTransactionsBuilder()
                .putFrom(Date(System.currentTimeMillis() - DateUtils.DAY_IN_MILLIS * 30))
                .putTo(Date())
                .putTransactionFilter(TypeTransactionFilter(if (type == Category.EXPENSE) Transaction.EXPENSE else Transaction.INCOME))
                .listInternal()

        val counts = ArrayMap<Long, Int>()
        for (t in transactions) {
            val count = counts[t.categoryId]
            counts.put(t.categoryId, if (count == null) 1 else count + 1)
        }

        for (c in categories) {
            if (!counts.containsKey(c.id)) counts.put(c.id, 0)
        }
        return categories.sortedByDescending { cat -> counts[cat.id] }
    }

    private fun loadCategoryData(id: Long, datePair: Pair<Date, Date>): CategoryPeriodData {
        val data = CategoryPeriodData()
        data.from = datePair.first
        data.to = datePair.second
        data.amount = 0.0

        val transactions = dataManager.transactionService
                .newListTransactionsBuilder()
                .putFrom(datePair.first)
                .putTo(datePair.second)
                .putCategoryId(id)
                .putConvertToMain(true)
                .listInternal()

        for (t in transactions) {
            val subcategoryId = t.subcategoryId
            if (subcategoryId != null) data.handleSubcategoryAmount(subcategoryId, t.amount)

            data.amount += t.amount
        }

        return data
    }

    private fun hasDependencies(subcategory: Subcategory): Boolean {
        val daoSession = dataManager.daoSession

        if (subcategory.globalId != null) return true

        var count = daoSession.transactionDao.queryBuilder().where(TransactionDao.Properties.SubcategoryId.eq(subcategory.id))
                .limit(1).count()
        if (count > 0) return true

        count = daoSession.transactionPatternDao.queryBuilder().where(TransactionPatternDao.Properties.SubcategoryId.eq(subcategory.id)).limit(1)
                .count()
        if (count > 0) return true

        count = daoSession.budgetPlanDependencyDao.queryBuilder()
                .where(BudgetPlanDependencyDao.Properties.RefType.eq(DataConstants.SUBCATEGORY_TYPE),
                        BudgetPlanDependencyDao.Properties.RefId.eq(subcategory.id.toString())).limit(1)
                .count()

        return count > 0
    }

    private fun hasDependencies(category: Category): Boolean {
        val daoSession = dataManager.daoSession
        if (category.globalId != null) return true

        var count = daoSession.transactionDao.queryBuilder().where(TransactionDao.Properties.CategoryId.eq(category.id)).limit(1).count()
        if (count > 0) return true

        count = daoSession.transactionPatternDao.queryBuilder().where(TransactionPatternDao.Properties.CategoryId.eq(category.id)).limit(1).count()
        if (count > 0) return true

        count = daoSession.budgetPlanDependencyDao.queryBuilder().where(BudgetPlanDependencyDao.Properties.RefType.eq(DataConstants.CATEGORY_TYPE),
                BudgetPlanDependencyDao.Properties.RefId.eq(category.id.toString())).limit(1).count()
        if (count > 0) return true

        if (!category.subcategories.isEmpty()) {
            for (s in category.subcategories) {
                count = daoSession.budgetPlanDependencyDao.queryBuilder()
                        .where(BudgetPlanDependencyDao.Properties.RefType.eq(DataConstants.SUBCATEGORY_TYPE),
                                BudgetPlanDependencyDao.Properties.RefId.eq(s.id.toString()))
                        .count()

                if (count > 0) return true
            }
        }

        return false
    }

    private fun convertCategoryBudgetToSubcategory(oldCategoryId: Long, subcategory: Subcategory, daoSession: DaoSession) {
        var dependencies = daoSession.budgetPlanDependencyDao.queryBuilder().where(BudgetPlanDependencyDao.Properties.RefType.eq(DataConstants.CATEGORY_TYPE),
                BudgetPlanDependencyDao.Properties.RefId.eq(oldCategoryId.toString())).list()

        for (d in dependencies) {
            d.refId = subcategory.id.toString()
            d.refType = DataConstants.SUBCATEGORY_TYPE
            daoSession.update(d)

            val plan = daoSession.budgetPlanDao.load(d.planId)
            if (plan.globalId != null) {
                plan.synced = false
                daoSession.update(plan)
            }
        }

        val category = daoSession.categoryDao.load(oldCategoryId)
        if (!category.subcategories.isEmpty()) {
            for (s in category.subcategories) {
                dependencies = daoSession.budgetPlanDependencyDao.queryBuilder().where(BudgetPlanDependencyDao.Properties.RefType.eq(DataConstants.SUBCATEGORY_TYPE),
                        BudgetPlanDependencyDao.Properties.RefId.eq(s.id.toString())).list()
                for (d in dependencies) {
                    d.refId = subcategory.id.toString()
                    d.refType = DataConstants.SUBCATEGORY_TYPE
                    daoSession.update(d)

                    val plan = daoSession.budgetPlanDao.load(d.planId)
                    if (plan.globalId != null) {
                        plan.synced = false
                        daoSession.update(plan)
                    }
                }
            }
        }
    }

    private fun convertCategoryPatternsToSubcategory(oldCategoryId: Long, subcategory: Subcategory, daoSession: DaoSession) {
        val patterns = daoSession.transactionPatternDao.queryBuilder().where(TransactionPatternDao.Properties.CategoryId.eq(oldCategoryId)).list()
        for (p in patterns) {
            p.categoryId = subcategory.categoryId
            p.subcategoryId = subcategory.id
            if (p.globalId != null) {
                p.synced = false
            }
            daoSession.update(p)
        }
    }

    private fun convertCategoryTransactionsToSubcategory(oldCategoryId: Long, subcategory: Subcategory, daoSession: DaoSession) {
        val transactions = daoSession.transactionDao.queryBuilder().where(TransactionDao.Properties.CategoryId.eq(oldCategoryId)).list()

        for (t in transactions) {
            t.categoryId = subcategory.categoryId
            t.subcategoryId = subcategory.id
            if (t.globalId != null) {
                t.synced = false
            }
            daoSession.update(t)
        }
    }

    companion object {

        val MAX_FREQUENTLY_ITEMS = 7
    }
}
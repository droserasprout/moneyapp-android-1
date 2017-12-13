package com.cactusteam.money.data.service

import com.cactusteam.money.data.DataManager
import com.cactusteam.money.data.dao.*
import com.cactusteam.money.data.model.TransactionSearch
import com.cactusteam.money.data.service.builder.SearchTransactionBuilder
import com.cactusteam.money.data.service.builder.TransactionBuilder
import com.cactusteam.money.sync.SyncConstants
import de.greenrobot.dao.query.QueryBuilder
import java.util.*

/**
 * @author vpotapenko
 */
abstract class TransactionInternalService(dataManager: DataManager) : BaseService(dataManager) {

    fun searchInternal(transactionSearch: TransactionSearch): TransactionSearch {
        val daoSession = dataManager.daoSession

        val queryBuilder = daoSession.transactionDao.queryBuilder()

        queryBuilder.where(TransactionDao.Properties.Comment.like(transactionSearch.query))
        queryBuilder.offset(transactionSearch.offset)
        queryBuilder.limit(transactionSearch.limit)
        queryBuilder.orderDesc(TransactionDao.Properties.Date)

        transactionSearch.transactions.clear()
        transactionSearch.transactions.addAll(queryBuilder.list())

        if (transactionSearch.transactions.size < transactionSearch.limit) {
            transactionSearch.hasMore = false
        } else {
            transactionSearch.hasMore = true
            transactionSearch.offset += transactionSearch.transactions.size
        }
        return transactionSearch
    }

    fun createTransactionFromPatternInternal(patternId: Long, amount: Double): Transaction {
        val pattern = dataManager.daoSession.transactionPatternDao.load(patternId)

        val b = dataManager.transactionService
                .newTransactionBuilder()
                .putDate(Date())
                .putSourceAccountId(pattern.sourceAccountId)
                .putComment(pattern.comment)
                .putType(pattern.type)
                .putAmount(amount)
                .putCategoryId(pattern.categoryId)
                .putSubcategoryId(pattern.subcategoryId)
                .putDestAccountId(pattern.destAccountId)
                .putDestAmount(pattern.destAmount)

        for (tag in pattern.tags) {
            b.putTag(tag.tag.name)
        }
        return b.createInternal()
    }

    fun copyTransactionInternal(id: Long): Transaction {
        val daoSession = dataManager.daoSession
        val source = daoSession.transactionDao.load(id)

        val newTransaction = source.copy()
        daoSession.insert(newTransaction)

        for (tag in source.tags) {
            val newTag = TransactionTag()
            newTag.tagId = tag.tagId
            newTag.transactionId = newTransaction.id

            daoSession.insert(newTag)
        }

        return newTransaction
    }

    fun updateTransactionInternal(builder: TransactionBuilder): Transaction {
        val daoSession = dataManager.daoSession

        val transaction = daoSession.transactionDao.load(builder.id)
        if (builder.globalId == null && transaction.globalId != null) {
            builder.putGlobalId(transaction.globalId).putSynced(false)
        }
        fillTransaction(builder, transaction)
        daoSession.update(transaction)

        for (transactionTag in transaction.tags) {
            val name = transactionTag.tag.name
            if (builder.tags.contains(name)) {
                builder.tags.remove(name)
            } else {
                daoSession.delete(transactionTag)
            }
        }
        createTags(transaction, builder.tags)

        transaction.resetTags()
        transaction.tags

        handleDebtReference(transaction)
        handlePlanning(transaction)
        if (transaction.status != Transaction.STATUS_WAITING_CONFIRMATION) {
            clearWaitingNotes(transaction)
        }

        return transaction
    }

    private fun clearWaitingNotes(transaction: Transaction) {
        val noteService = dataManager.noteService
        val ref = noteService.createTransactionRef(transaction)
        noteService.deleteNoteByRefInternal(ref)
    }

    private fun handlePlanning(transaction: Transaction) {
        if (transaction.status == Transaction.STATUS_PLANNING) {
            getApplication().scheduler.updateAlarm()
        }
    }

    fun getTransactionInternal(id: Long): Transaction {
        return dataManager.daoSession.transactionDao.load(id)
    }

    fun deleteTransactionInternal(id: Long) {
        val daoSession = dataManager.daoSession

        val transaction = daoSession.transactionDao.load(id)
        for (tag in transaction.tags) {
            daoSession.delete(tag)
        }
        if (transaction.globalId != null) {
            val trash = Trash()
            trash.type = SyncConstants.TRANSACTION_TYPE
            trash.globalId = transaction.globalId
            daoSession.insert(trash)
        }
        daoSession.delete(transaction)
        clearWaitingNotes(transaction)
    }

    fun generateRandomTransactionsInternal() {
        val random = Random()

        dataManager.daoSession.runInTx({
            createTransactions(Category.EXPENSE, 4000, 1000, random)
        })
        dataManager.daoSession.runInTx({
            createTransactions(Category.INCOME, 200, 20000, random)
        })
    }

    fun countTransactionInternal(builder: SearchTransactionBuilder): Long {
        val queryBuilder = prepareQueryBuilder(builder)
        return queryBuilder.count()
    }

    fun listTransactionInternal(builder: SearchTransactionBuilder): List<Transaction> {
        val queryBuilder = prepareQueryBuilder(builder)
        queryBuilder.orderDesc(TransactionDao.Properties.Date)

        var list = queryBuilder.list()
        if (builder.filter != null) {
            list = list.filter { t -> builder.filter!!.allow(t) }
        }

        if (builder.convertToMainCurrency) {
            val mainCurrencyCode = getApplication().appPreferences.mainCurrencyCode
            for (transaction in list) {
                var currencyCode = transaction.sourceAccount.currencyCode
                var amount = transaction.amount
                if (currencyCode != mainCurrencyCode) {
                    val rate = loadCurrencyRate(currencyCode, mainCurrencyCode)
                    amount = rate?.convertTo(amount, mainCurrencyCode) ?: amount
                }
                transaction.amountInMainCurrency = amount

                if (transaction.type == Transaction.TRANSFER) {
                    if (transaction.destAmount != null) {
                        currencyCode = transaction.destAccount.currencyCode
                        amount = transaction.destAmount!!
                        if (currencyCode != mainCurrencyCode) {
                            val rate = loadCurrencyRate(currencyCode, mainCurrencyCode)
                            amount = rate?.convertTo(amount, mainCurrencyCode) ?: amount
                        }
                        transaction.destAmountInMainCurrency = amount
                    }
                }
            }
        }
        return list
    }

    private fun prepareQueryBuilder(builder: SearchTransactionBuilder): QueryBuilder<Transaction> {
        val daoSession = dataManager.daoSession

        val queryBuilder = daoSession.transactionDao.queryBuilder()
        if (builder.accountId != null) {
            queryBuilder.whereOr(TransactionDao.Properties.SourceAccountId.eq(builder.accountId),
                    TransactionDao.Properties.DestAccountId.eq(builder.accountId))
        }
        if (builder.categoryId != null) {
            queryBuilder.where(TransactionDao.Properties.CategoryId.eq(builder.categoryId))
        }
        if (builder.subcategoryId != null) {
            queryBuilder.where(TransactionDao.Properties.SubcategoryId.eq(builder.subcategoryId))
        }
        if (builder.from != null) {
            queryBuilder.where(TransactionDao.Properties.Date.ge(builder.from))
        }
        if (builder.to != null) {
            queryBuilder.where(TransactionDao.Properties.Date.le(builder.to))
        }
        if (builder.ref != null) {
            queryBuilder.where(TransactionDao.Properties.Ref.eq(builder.ref))
        }
        if (builder.notStatus != null) {
            queryBuilder.where(TransactionDao.Properties.Status.notEq(builder.notStatus))
        } else {
            queryBuilder.where(TransactionDao.Properties.Status.eq(builder.status))
        }
        if (builder.max != null) {
            queryBuilder.limit(builder.max!!)
        }

        return queryBuilder
    }

    fun createTransactionInternal(builder: TransactionBuilder): Transaction {
        val transaction = Transaction()
        transaction.type = builder.type
        fillTransaction(builder, transaction)

        dataManager.daoSession.insert(transaction)

        createTags(transaction, builder.tags)

        handleDebtReference(transaction)
        handlePlanning(transaction)

        return transaction
    }

    private fun handleDebtReference(transaction: Transaction) {
        if (transaction.type == Transaction.TRANSFER) return

        val debtService = dataManager.debtService
        if (transaction.tags.isNotEmpty()) {
            val debt: Debt? = transaction.tags
                    .map { it.tag.name }
                    .map { debtService.findActiveDebtsByNameInternal(it) }
                    .filter { it.size == 1 }
                    .firstOrNull()?.get(0)

            if (debt != null) {
                transaction.ref = String.format(Debt.DEBT_REF_PATTERN, debt.id)
                dataManager.daoSession.update(transaction)
            }
        } else {
            if (transaction.tags.isEmpty() && transaction.ref?.startsWith(Debt.DEBT_REF_START) ?: false) {
                transaction.ref = null
                dataManager.daoSession.update(transaction)
            }
        }
    }

    protected fun savePref(sourceAccountId: Long, categoryId: Long?) {
        val appPreferences = getApplication().appPreferences

        appPreferences.lastAccountId = sourceAccountId
        if (categoryId != null) appPreferences.lastCategoryId = categoryId
    }

    private fun fillTransaction(builder: TransactionBuilder, transaction: Transaction) {
        transaction.date = builder.date
        transaction.sourceAccountId = builder.sourceAccountId
        transaction.amount = builder.amount
        transaction.categoryId = builder.categoryId
        transaction.subcategoryId = builder.subcategoryId
        transaction.destAccountId = builder.destAccountId
        transaction.destAmount = builder.destAmount
        transaction.comment = builder.comment
        transaction.ref = builder.ref
        transaction.status = builder.status
        transaction.globalId = builder.globalId
        transaction.synced = builder.synced
    }

    private fun createTags(transaction: Transaction, tags: MutableList<String>) {
        for (tag in tags) {
            createTag(transaction, tag)
        }
    }

    private fun createTag(transaction: Transaction, tagName: String) {
        val daoSession = dataManager.daoSession
        val list = daoSession.tagDao.queryBuilder().where(TagDao.Properties.Name.eq(tagName)).limit(1).list()

        val tag: Tag
        if (list == null || list.isEmpty()) {
            tag = Tag()
            tag.name = tagName
            tag.updated = Date()

            daoSession.insert(tag)
        } else {
            tag = list[0]
            tag.updated = Date()

            daoSession.update(tag)
        }

        val transactionTag = TransactionTag()
        transactionTag.tag = tag
        transactionTag.transactionId = transaction.id

        daoSession.insert(transactionTag)
    }

    private fun loadCurrencyRate(currencyCode1: String, currencyCode2: String): CurrencyRate? {
        return dataManager.currencyService.getRateInternal(currencyCode1, currencyCode2)
    }

    private fun createTransactions(type: Int, number: Int, startAmount: Int, random: Random) {
        var count = number
        try {
            val categories = dataManager.categoryService.getCategoriesInternal(type)
            if (categories.isEmpty()) return

            val accounts = dataManager.accountService.getAccountsInternal()
            if (accounts.isEmpty()) return

            val calendar = Calendar.getInstance()
            while (count > 0) {
                calendar.timeInMillis = System.currentTimeMillis()
                calendar.add(Calendar.DATE, -random.nextInt(365))
                calendar.set(Calendar.HOUR_OF_DAY, random.nextInt(24))

                val c = categories[random.nextInt(categories.size)]
                val a = accounts[random.nextInt(accounts.size)]

                val amount = (startAmount + random.nextInt(startAmount)).toDouble()

                val b = dataManager.transactionService
                        .newTransactionBuilder()
                        .putType(if (type == Category.EXPENSE) Transaction.EXPENSE else Transaction.INCOME)
                        .putDate(calendar.time)
                        .putSourceAccountId(a.id)
                        .putAmount(amount)
                        .putCategoryId(c.id)

                val subcategories = c.subcategories
                if (!subcategories.isEmpty()) {
                    val index = random.nextInt(subcategories.size + 1)
                    if (index < subcategories.size) {
                        b.putSubcategoryId(subcategories[index].id)
                    }
                }
                b.createInternal()

                count--
            }
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }
}
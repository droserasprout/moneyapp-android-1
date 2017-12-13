package com.cactusteam.money.data.service

import com.cactusteam.money.data.DataManager
import com.cactusteam.money.data.dao.Transaction
import com.cactusteam.money.data.dao.TransactionTag
import com.cactusteam.money.data.model.TransactionSearch
import com.cactusteam.money.data.service.builder.SearchTransactionBuilder
import com.cactusteam.money.data.service.builder.TransactionBuilder
import rx.Observable
import rx.lang.kotlin.observable

/**
 * @author vpotapenko
 */
class TransactionService(dataManager: DataManager) : TransactionInternalService(dataManager) {

    fun search(transactionSearch: TransactionSearch): Observable<TransactionSearch> {
        return wrap<TransactionSearch> { s ->
            s.onNext(searchInternal(transactionSearch))
        }
    }

    fun createTransactionFromPattern(patternId: Long, amount: Double): Observable<Transaction> {
        return wrap<Transaction> { s ->
            val t = createTransactionFromPatternInternal(patternId, amount)
            dataManager.fireBalanceChanged()

            s.onNext(t)
        }
    }

    fun copyTransaction(id: Long): Observable<Transaction> {
        return wrap<Transaction> { s ->
            val t = copyTransactionInternal(id)
            dataManager.fireBalanceChanged()

            s.onNext(t)
        }
    }

    fun getTransaction(id: Long): Observable<Transaction> {
        return wrap<Transaction> { s ->
            s.onNext(getTransactionInternal(id))
        }
    }

    fun deleteTransaction(id: Long, fireEvent: Boolean = true): Observable<Unit> {
        return wrap<Unit> { s ->
            deleteTransactionInternal(id)
            if (fireEvent) dataManager.fireBalanceChanged()
        }
    }

    fun generateRandomTransactions(): Observable<Unit> {
        return wrap<Unit> { s ->
            generateRandomTransactionsInternal()
            dataManager.fireBalanceChanged()
        }
    }

    fun newListTransactionsBuilder(): SearchTransactionBuilder {
        return SearchTransactionBuilder(this)
    }

    fun newTransactionBuilder(): TransactionBuilder {
        return TransactionBuilder(this)
    }

    fun newTransactionBuilder(t: Transaction): TransactionBuilder {
        val builder = TransactionBuilder(this)
                .putId(t.id)
                .putType(t.type)
                .putDate(t.date)
                .putComment(t.comment)
                .putSourceAccountId(t.sourceAccountId)
                .putAmount(t.amount)
                .putCategoryId(t.categoryId)
                .putSubcategoryId(t.subcategoryId)
                .putDestAccountId(t.destAccountId)
                .putDestAmount(t.destAmount)
                .putRef(t.ref)
                .putStatus(t.status)
                .putGlobalId(t.globalId)
                .putSynced(t.synced)

        for (tag in t.tags) {
            builder.putTag(tag.tag.name)
        }
        return builder
    }

    fun listTransaction(builder: SearchTransactionBuilder): Observable<List<Transaction>> {
        return wrap<List<Transaction>> { s ->
            s.onNext(listTransactionInternal(builder))
        }
    }

    fun createTransaction(builder: TransactionBuilder): Observable<Transaction> {
        return wrap<Transaction> { s ->
            val t = createTransactionInternal(builder)
            savePref(builder.sourceAccountId, builder.categoryId)

            dataManager.fireBalanceChanged()

            s.onNext(t)
        }
    }

    fun updateTransaction(builder: TransactionBuilder): Observable<Transaction> {
        return wrap<Transaction> { s ->
            val t = updateTransactionInternal(builder)
            dataManager.fireBalanceChanged()

            s.onNext(t)
        }
    }
}
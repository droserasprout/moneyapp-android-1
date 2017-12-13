package com.cactusteam.money.data.service.builder

import com.cactusteam.money.data.dao.Transaction
import com.cactusteam.money.data.service.TransactionService
import rx.Observable

/**
 * @author vpotapenko
 */
class TransactionBuilder(val parent: TransactionService) : BaseTransactionBuilder<TransactionBuilder>() {

    fun create(): Observable<Transaction> {
        return parent.createTransaction(this)
    }

    fun createInternal(): Transaction {
        return parent.createTransactionInternal(this)
    }

    fun update(): Observable<Transaction> {
        return parent.updateTransaction(this)
    }

    fun updateInternal(): Transaction {
        return parent.updateTransactionInternal(this)
    }

    fun clone(): TransactionBuilder {
        val b = TransactionBuilder(parent)

        b._name = _name
        b._id = _id
        b._type = _type
        b._date = _date
        b._sourceAccountId = _sourceAccountId
        b._amount = _amount
        b._categoryId = _categoryId
        b._subcategoryId = _subcategoryId
        b._destAccountId = _destAccountId
        b._destAmount = _destAmount
        b._comment = _comment
        b._rate = _rate
        b._ref = _ref
        b._globalId = _globalId
        b._synced = _synced
        b._status = _status

        b._tags.addAll(_tags)

        return b
    }
}
package com.cactusteam.money.data

import com.cactusteam.money.data.dao.DaoSession
import com.cactusteam.money.data.dao.Transaction
import com.cactusteam.money.data.dao.TransactionDao
import de.greenrobot.dao.query.WhereCondition
import java.util.*

/**
 * @author vpotapenko
 */
class AccountTotalLoader(private val accountId: Long, private val daoSession: DaoSession) {

    var expense = 0.0
        private set
    var income = 0.0
        private set
    var transfer = 0.0
        private set

    val total: Double
        get() = DataUtils.round(income - expense + transfer, 2)

    fun load(from: Date?, to: Date?) {
        expense = loadExpense(from, to)
        income = loadIncome(from, to)
        transfer = loadTransfer(from, to)
    }

    private fun loadTransfer(from: Date?, to: Date?): Double {
        val receive = loadReceiveTransfer(from, to)
        val send = loadSendTransfer(from, to)
        return receive - send
    }

    private fun loadSendTransfer(from: Date?, to: Date?): Double {
        return loadTotalByType(from, to, Transaction.TRANSFER, true)
    }

    private fun loadReceiveTransfer(from: Date?, to: Date?): Double {
        return loadTotalByType(from, to, Transaction.TRANSFER, false)
    }

    private fun loadIncome(from: Date?, to: Date?): Double {
        return loadTotalByType(from, to, Transaction.INCOME, true)
    }

    private fun loadExpense(from: Date?, to: Date?): Double {
        return loadTotalByType(from, to, Transaction.EXPENSE, true)
    }

    private fun loadTotalByType(from: Date?, to: Date?, type: Int, source: Boolean): Double {
        val additionalConditions = ArrayList<WhereCondition>()

        if (source) {
            additionalConditions.add(TransactionDao.Properties.SourceAccountId.eq(accountId))
        } else {
            additionalConditions.add(TransactionDao.Properties.DestAccountId.eq(accountId))
        }

        if (from != null) {
            additionalConditions.add(TransactionDao.Properties.Date.ge(from))
        }
        if (to != null) {
            additionalConditions.add(TransactionDao.Properties.Date.le(to))
        }

        val queryBuilder = daoSession.transactionDao.queryBuilder()
        queryBuilder.where(TransactionDao.Properties.Type.eq(type),
                *additionalConditions.toTypedArray())

        val list = queryBuilder.list()

        var total = 0.0
        for (transaction in list) {
            val amount = if (source) transaction.amount else transaction.destAmount
            total += amount
        }
        return total
    }
}

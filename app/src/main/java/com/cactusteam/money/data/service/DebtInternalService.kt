package com.cactusteam.money.data.service

import com.cactusteam.money.R
import com.cactusteam.money.data.DataManager
import com.cactusteam.money.data.dao.*
import com.cactusteam.money.data.service.builder.DebtBuilder
import com.cactusteam.money.sync.SyncConstants
import java.util.*

/**
 * @author vpotapenko
 */
abstract class DebtInternalService(dataManager: DataManager) : BaseService(dataManager) {

    fun deleteDebtNoteInternal(debtNoteId: Long, forceReset: Boolean) {
        val daoSession = dataManager.daoSession
        val debtNote = daoSession.debtNoteDao.load(debtNoteId)
        if (debtNote != null) {
            if (debtNote.globalId != null) {
                val trash = Trash()
                trash.type = SyncConstants.DEBT_NOTE_TYPE
                trash.globalId = debtNote.globalId
                daoSession.insert(trash)
            }
            daoSession.delete(debtNote)

            val debt = daoSession.debtDao.load(debtNote.debtId)
            if (forceReset && debt != null) {
                debt.resetNotes()
                debt.notes
            }
        }
    }

    fun updateDebtNoteInternal(debtNoteId: Long, text: String): DebtNote? {
        val daoSession = dataManager.daoSession
        val debtNote = daoSession.debtNoteDao.load(debtNoteId)
        if (debtNote != null) {
            debtNote.text = text
            if (debtNote.globalId != null) debtNote.synced = false

            daoSession.update(debtNote)
        }
        return debtNote
    }

    fun createDebtNoteInternal(debtId: Long, text: String, forceReset: Boolean): DebtNote {
        val daoSession = dataManager.daoSession

        val note = DebtNote()
        note.date = Date()
        note.debtId = debtId
        note.text = text
        daoSession.insert(note)

        val debt = daoSession.debtDao.load(debtId)
        if (forceReset && debt != null) {
            debt.resetNotes()
            debt.notes
        }

        return note
    }

    fun getCurrentDebtsInternal(): List<Debt> {
        val daoSession = dataManager.daoSession

        val list = daoSession.debtDao
                .queryBuilder()
                .where(DebtDao.Properties.Finished.eq(false))
                .orderAsc(DebtDao.Properties.Till)
                .list()
        for (debt in list) {
            setCurrentDebtAmount(debt)
        }
        return list
    }

    fun getDebtsInternal(): List<Debt> {
        val daoSession = dataManager.daoSession

        val list = daoSession.debtDao.queryBuilder().orderAsc(DebtDao.Properties.Till).list()
        for (debt in list) {
            setCurrentDebtAmount(debt)
        }
        return list
    }

    fun updateDebtTimeInternal(debtId: Long, till: Date): Debt {
        val daoSession = dataManager.daoSession
        val debt = daoSession.debtDao.load(debtId)
        debt.till = till
        if (debt.globalId != null) debt.synced = false

        daoSession.update(debt)
        return debt
    }

    fun changeDebtAmountInternal(debtId: Long, amount: Double): Debt {
        val daoSession = dataManager.daoSession
        val debt = daoSession.debtDao.load(debtId)
        if (amount != 0.0) {
            val account = daoSession.accountDao.load(debt.accountId)

            val type = if (amount > 0) Category.INCOME else Category.EXPENSE
            val categoryId = findDebtCategory(type)

            dataManager.transactionService.newTransactionBuilder()
                    .putType(if (type == Category.EXPENSE) Transaction.EXPENSE else Transaction.INCOME)
                    .putAmount(Math.abs(amount))
                    .putCategoryId(categoryId)
                    .putSourceAccountId(account.id)
                    .putDate(Date())
                    .putTag(debt.name)
                    .putRef(String.format(Debt.DEBT_REF_PATTERN, debtId))
                    .createInternal()
        }

        return debt
    }

    fun closeDebtInternal(debtId: Long): Debt {
        val daoSession = dataManager.daoSession
        val debt = daoSession.debtDao.load(debtId)
        setCurrentDebtAmount(debt)

        var amount = debt.amount
        if (amount != 0.0) {
            val account = daoSession.accountDao.load(debt.accountId)
            if (debt.currencyCode != account.currencyCode) {

                val rate = dataManager.currencyService.getRateInternal(debt.currencyCode, account.currencyCode)
                amount = rate?.convertTo(amount, account.currencyCode) ?: amount
            }

            val categoryId = findDebtCategory(if (amount < 0) Category.EXPENSE else Category.INCOME)

            dataManager.transactionService
                    .newTransactionBuilder()
                    .putType(if (amount < 0) Transaction.EXPENSE else Transaction.INCOME)
                    .putAmount(Math.abs(amount))
                    .putCategoryId(categoryId)
                    .putSourceAccountId(account.id)
                    .putDate(Date())
                    .putTag(debt.name)
                    .putRef(String.format(Debt.DEBT_REF_PATTERN, debtId))
                    .createInternal()
        }
        debt.finished = true
        if (debt.globalId != null) debt.synced = false

        daoSession.update(debt)

        return debt
    }

    fun updateDebtInternal(builder: DebtBuilder): Debt {
        val daoSession = dataManager.daoSession
        val debt = daoSession.debtDao.load(builder.id)

        if (builder.globalId == null && debt.globalId != null) {
            builder.putGlobalId(debt.globalId).putSynced(false)
        }
        fillDebt(debt, builder)

        daoSession.update(debt)
        debt.account

        return debt
    }

    private fun fillDebt(debt: Debt, builder: DebtBuilder) {
        debt.name = builder.name
        debt.phone = builder.phone
        debt.contactId = builder.contactId
        debt.accountId = builder.accountId!!
        debt.till = builder.till
        debt.start = builder.start
        debt.finished = builder.finished ?: false
        debt.globalId = builder.globalId
        debt.synced = builder.synced
    }

    fun createDebtInternal(builder: DebtBuilder): Debt {
        val debt = Debt()
        fillDebt(debt, builder)
        dataManager.daoSession.insert(debt)

        return debt
    }

    fun deleteDebtInternal(id: Long, removeTransactions: Boolean) {
        val daoSession = dataManager.daoSession

        val transactions = dataManager.transactionService
                .newListTransactionsBuilder()
                .putRef(String.format(Debt.DEBT_REF_PATTERN, id))
                .listInternal()
        for (t in transactions) {
            if (removeTransactions) {
                dataManager.transactionService.deleteTransactionInternal(t.id)
            } else {
                t.ref = null
                daoSession.update(t)
            }
        }

        val debt = daoSession.debtDao.load(id)
        debt.notes.forEach { deleteDebtNoteInternal(it.id, false) }

        if (debt.globalId != null) {
            val trash = Trash()
            trash.type = SyncConstants.DEBT_TYPE
            trash.globalId = debt.globalId
            daoSession.insert(trash)
        }

        daoSession.debtDao.delete(debt)
        dataManager.fireBalanceChanged()
    }

    fun getDebtInternal(id: Long): Debt {
        val debt = dataManager.daoSession.debtDao.load(id)
        setCurrentDebtAmount(debt)

        return debt
    }

    fun findActiveDebtsByNameInternal(name: String): List<Debt> {
        return dataManager.daoSession.debtDao
                .queryBuilder()
                .where(DebtDao.Properties.Name.eq(name))
                .list()
                .filterNot { it.finished }
    }

    fun findDebtCategory(type: Int, readOnly: Boolean = false): Long {
        val categoryType = if (type == Transaction.INCOME) Category.INCOME else Category.EXPENSE
        val name = getApplication().resources.getString(R.string.debts_title)
        return findCategoryByName(categoryType, name, "finance_006.png", readOnly)
    }

    private fun setCurrentDebtAmount(debt: Debt) {
        val transactions = dataManager.transactionService
                .newListTransactionsBuilder()
                .putRef(String.format(Debt.DEBT_REF_PATTERN, debt.id))
                .putConvertToMain(true)
                .listInternal()
                .reversed()

        val currencyCode: String? = debt.account.currencyCode

        var amount = 0.0
        var amountInMain = 0.0
        var hasAnother = false

        for (transaction in transactions) {
            hasAnother = hasAnother || (currencyCode != transaction.sourceAccount.currencyCode)

            if (transaction.type == Transaction.EXPENSE) {
                amount += transaction.amount
                amountInMain += transaction.amountInMainCurrency
            } else if (transaction.type == Transaction.INCOME) {
                amount -= transaction.amount
                amountInMain -= transaction.amountInMainCurrency
            }
        }

        if (hasAnother || transactions.isEmpty()) {
            debt.currencyCode = getApplication().appPreferences.mainCurrencyCode
            debt.amount = amountInMain
        } else {
            debt.currencyCode = currencyCode
            debt.amount = amount
        }
    }
}
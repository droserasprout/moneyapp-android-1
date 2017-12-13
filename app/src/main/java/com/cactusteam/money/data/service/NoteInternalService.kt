package com.cactusteam.money.data.service

import com.cactusteam.money.R
import com.cactusteam.money.data.DataManager
import com.cactusteam.money.data.dao.*

/**
 * @author vpotapenko
 */
abstract class NoteInternalService(dataManager: DataManager) : BaseService(dataManager) {

    fun deleteNoteByRefInternal(ref: String) {
        val daoSession = dataManager.daoSession
        daoSession.noteDao.queryBuilder()
                .where(NoteDao.Properties.Ref.eq(ref))
                .list()
                .forEach {
                    daoSession.delete(it)
                }

    }

    fun createDebtOverNoteInternal(debt: Debt, almost: Boolean) {
        val message = getApplication().getString(if (almost) R.string.debt_is_almost_over else R.string.debt_is_over, debt.name)
        createNoteInternal(message, String.format(Note.DEBT_REF_PATTERN, debt.id))
    }

    fun createBudgetOverNoteInternal(plan: BudgetPlan) {
        val message = getApplication().getString(R.string.budget_is_over_pattern, plan.name)
        createNoteInternal(message, String.format(Note.BUDGET_REF_PATTERN, plan.id))
    }

    fun createSyncErrorNoteInternal(message: String) {
        createNoteInternal(message, Note.SYNC_ERROR_REF)
    }

    fun createTransactionNoteInternal(transaction: Transaction) {
        val ref = createTransactionRef(transaction)
        createNoteInternal(getApplication().getString(R.string.transaction_waiting_for_confirmation), ref)
    }

    fun createNoteInternal(description: String?, ref: String?) {
        val daoSession = dataManager.daoSession

        val note = Note()
        note.description = description ?: "Something went wrong"
        note.ref = ref

        daoSession.insert(note)
    }

    fun createTransactionRef(transaction: Transaction): String {
        return String.format(Note.TRANSACTION_REF_PATTERN, transaction.id)
    }

    fun deleteNoteInternal(id: Long) {
        dataManager.daoSession.noteDao.deleteByKey(id)
    }

    fun getNotesInternal(): List<Note> {
        return dataManager.daoSession.noteDao.loadAll()
    }
}
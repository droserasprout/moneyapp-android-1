package com.cactusteam.money.data

import com.cactusteam.money.app.MoneyApp
import com.cactusteam.money.data.dao.BudgetPlan
import com.cactusteam.money.data.dao.Note
import org.apache.commons.lang3.time.DateUtils
import java.util.*

/**
 * @author vpotapenko
 */
class DatabaseOpener(private val dataManager: DataManager) {

    fun prepareDatabase() {
        try {
            clearObsoleteNotes()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            handleLatePlanningTransactions()
        } catch (e: Exception) {
            e.printStackTrace()
            dataManager.noteService.createNoteInternal(e.message, null)
        }

        try {
            handleAutoBackup()
        } catch (e: Exception) {
            e.printStackTrace()
            dataManager.noteService.createNoteInternal(e.message, null)
        }

        try {
            doBudgetOperation()
        } catch (e: Exception) {
            e.printStackTrace()
            dataManager.noteService.createNoteInternal(e.message, null)
        }

        try {
            checkDebts()
        } catch (e: Exception) {
            e.printStackTrace()
            dataManager.noteService.createNoteInternal(e.message, null)
        }
    }

    private fun doBudgetOperation() {
        val plans = dataManager.budgetService.getBudgetsInternal()

        for (plan in plans) {
            if (plan.isFinished) {
                if (plan.type == BudgetPlan.PERIODICAL_TYPE && plan.next == null)
                    createNextBudget(plan)
            } else {
                val amount = dataManager.budgetService.getBudgetAmountInternal(plan.id)
                if (amount > plan.limit) dataManager.noteService.createBudgetOverNoteInternal(plan)
            }
        }
    }

    private fun createNextBudget(plan: BudgetPlan) {
        var b = dataManager.budgetService
                .newBudgetBuilder()
                .putName(plan.name)
                .putLimit(plan.limit).putType(plan.type)

        val moneyApp = MoneyApp.instance
        val period = moneyApp.period
        val fullCurrent = period.fullCurrent
        b.putFrom(fullCurrent.first).putTo(fullCurrent.second)

        for (dependency in plan.dependencies) {
            b.putDependency(dependency.refType, dependency.refId)
        }
        val newPlan = b.createInternal()

        b = dataManager.budgetService
                .newBudgetBuilder()
                .putId(plan.id)
                .putName(plan.name)
                .putLimit(plan.limit)
                .putFrom(plan.start)
                .putTo(plan.finish)
                .putType(plan.type)
                .putNextId(newPlan.id)
        for (dependency in plan.dependencies) {
            b.putDependency(dependency.refType, dependency.refId)
        }

        b.updateInternal()
    }

    private fun handleAutoBackup() {
        if (getApplication().appPreferences.isAutoBackup) {
            dataManager.backupService.createAutoBackupInternal()
        }
    }

    private fun clearObsoleteNotes() {
        val daoSession = dataManager.daoSession
        val notes = daoSession.noteDao.loadAll()
        notes
                .filter { it.ref == null || !it.ref.startsWith(Note.TRANSACTION_REF_START) }
                .forEach { daoSession.delete(it) }
    }

    private fun handleLatePlanningTransactions() {
        dataManager.systemService.handleLatePlanningTransactions()
    }

    private fun checkDebts() {
        val debts = dataManager.debtService.getCurrentDebtsInternal()
        val now = Date()
        debts
                .filter { it.amount != 0.0 }
                .forEach {
                    if (now.after(it.till)) {
                        dataManager.noteService.createDebtOverNoteInternal(it, false)
                    } else if (it.till.time - now.time < DateUtils.MILLIS_PER_DAY) {
                        dataManager.noteService.createDebtOverNoteInternal(it, true)
                    }
                }
    }

    private fun getApplication(): MoneyApp {
        return MoneyApp.instance
    }
}
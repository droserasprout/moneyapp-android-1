package com.cactusteam.money.data.service

import com.cactusteam.money.data.DataConstants
import com.cactusteam.money.data.DataManager
import com.cactusteam.money.data.dao.BudgetPlan
import com.cactusteam.money.data.dao.BudgetPlanDao
import com.cactusteam.money.data.dao.BudgetPlanDependency
import com.cactusteam.money.data.dao.Trash
import com.cactusteam.money.data.service.builder.BudgetBuilder
import com.cactusteam.money.sync.SyncConstants
import java.util.*

/**
 * @author vpotapenko
 */
abstract class BudgetInternalService(dataManager: DataManager) : BaseService(dataManager) {

    fun getCurrentBudgetsInternal(): List<BudgetPlan> {
        val now = Date()
        val list = dataManager.daoSession.getBudgetPlanDao().queryBuilder()
                .where(BudgetPlanDao.Properties.Finish.ge(now))
                .orderDesc(BudgetPlanDao.Properties.Name).list()
        for (plan in list) {
            plan.expense = getBudgetAmountInternal(plan.id)
        }
        return list
    }

    fun getBudgetsInternal(fillExpense: Boolean = false): List<BudgetPlan> {
        val budgetPlans = dataManager.daoSession.budgetPlanDao.queryBuilder()
                .orderDesc(BudgetPlanDao.Properties.Finish).list()
        val now = Date()
        for (plan in budgetPlans) {
            plan.isFinished = now.after(plan.finish)
            if (fillExpense) {
                plan.expense = getBudgetAmountInternal(plan.id)
            }
        }
        return budgetPlans
    }

    fun createBudgetInternal(builder: BudgetBuilder): BudgetPlan {
        val daoSession = dataManager.daoSession
        val plan = BudgetPlan()
        fillBudget(builder, plan)
        daoSession.insert(plan)

        for (pair in builder.dependencies) {
            val dependency = BudgetPlanDependency()
            dependency.planId = plan.id
            dependency.refType = pair.first
            dependency.refId = pair.second

            daoSession.insert(dependency)
        }
        return plan
    }

    fun updateBudgetInternal(builder: BudgetBuilder): BudgetPlan {
        val daoSession = dataManager.daoSession

        val plan = daoSession.budgetPlanDao.load(builder.id)
        if (builder.globalId == null && plan.globalId != null) {
            builder.putGlobalId(plan.globalId).putSynced(false)
        }

        fillBudget(builder, plan)
        daoSession.update(plan)

        val planDependencies = plan.dependencies
        if (builder.dependencies.size < planDependencies.size) {
            for (i in builder.dependencies.indices) {
                val pair = builder.dependencies[i]

                val dependency = planDependencies[i]
                dependency.refType = pair.first
                dependency.refId = pair.second

                daoSession.update(dependency)
            }

            for (i in builder.dependencies.size..planDependencies.size - 1) {
                val dependency = planDependencies[i]
                daoSession.delete(dependency)
            }
        } else {
            for (i in planDependencies.indices) {
                val pair = builder.dependencies[i]

                val dependency = planDependencies[i]
                dependency.refType = pair.first
                dependency.refId = pair.second

                daoSession.update(dependency)
            }

            for (i in planDependencies.size..builder.dependencies.size - 1) {
                val pair = builder.dependencies[i]

                val dependency = BudgetPlanDependency()
                dependency.planId = plan.id
                dependency.refType = pair.first
                dependency.refId = pair.second

                daoSession.insert(dependency)
            }
        }

        plan.resetDependencies()
        plan.dependencies

        return plan
    }

    fun deleteBudgetInternal(id: Long): Unit {
        val daoSession = dataManager.daoSession
        val plan = daoSession.budgetPlanDao.load(id)

        for (dependency in plan.dependencies) {
            daoSession.delete(dependency)
        }

        if (plan.globalId != null) {
            val trash = Trash()
            trash.type = SyncConstants.BUDGET_TYPE
            trash.globalId = plan.globalId
            daoSession.insert(trash)
        }
        daoSession.delete(plan)
    }

    fun updateBudgetLimitInternal(id: Long, limit: Double) {
        val daoSession = dataManager.daoSession
        val plan = daoSession.budgetPlanDao.load(id)

        plan.limit = limit
        if (plan.globalId != null) plan.synced = false

        daoSession.update(plan)
    }

    fun getBudgetAmountInternal(id: Long): Double {
        val daoSession = dataManager.daoSession
        val budgetPlan = daoSession.budgetPlanDao.load(id)

        val now = Date()
        val transactions = dataManager.transactionService
                .newListTransactionsBuilder()
                .putFrom(budgetPlan.start)
                .putTo(if (now.before(budgetPlan.finish)) now else budgetPlan.finish)
                .putConvertToMain(true)
                .putTransactionFilter(budgetPlan.createFilter())
                .listInternal()

        val amount = transactions.sumByDouble { t -> t.amountInMainCurrency }
        return amount
    }

    fun getBudgetInternal(id: Long): BudgetPlan {
        val budgetPlan = dataManager.daoSession.budgetPlanDao.load(id)

        val objects = budgetPlan.dependencyObjects
        objects.clear()

        for (dependency in budgetPlan.dependencies) {
            val obj = getObject(dependency)
            if (obj != null) objects.put(dependency, obj)
        }

        return budgetPlan
    }

    private fun getObject(dependency: BudgetPlanDependency): Any? {
        val daoSession = dataManager.daoSession

        when {
            dependency.refType == DataConstants.CATEGORY_TYPE ->
                return daoSession.categoryDao.load(dependency.refId.toLong())
            dependency.refType == DataConstants.SUBCATEGORY_TYPE ->
                return daoSession.subcategoryDao.load(dependency.refId.toLong())
            dependency.refType == DataConstants.TAG_TYPE ->
                return daoSession.tagDao.load(dependency.refId.toLong())
            else -> return null
        }
    }

    private fun fillBudget(builder: BudgetBuilder, plan: BudgetPlan) {
        plan.name = builder.name
        plan.limit = builder.limit
        plan.start = builder.from
        plan.finish = builder.to
        plan.type = builder.type
        plan.next = builder.nextId
        plan.globalId = builder.globalId
        plan.synced = builder.synced
    }
}
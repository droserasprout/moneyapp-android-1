package com.cactusteam.money.data.service.builder

import com.cactusteam.money.data.dao.BudgetPlan
import com.cactusteam.money.data.service.BudgetService
import rx.Observable
import java.util.*

/**
 * @author vpotapenko
 */
class BudgetBuilder(private val budgetService: BudgetService) {

    val id: Long? get() = _id
    val name: String? get() = _name
    val limit: Double get() = _limit!!
    val from: Date? get() = _from
    val to: Date? get() = _to
    val type: Int get() = _type!!
    val nextId: Long? get() = _nextId
    val globalId: Long? get() = _globalId
    val synced: Boolean? get() = _synced
    val dependencies: MutableList<Pair<Int, String>> = mutableListOf()

    private var _id: Long? = null
    private var _name: String? = null
    private var _limit: Double? = null
    private var _from: Date? = null
    private var _to: Date? = null
    private var _type: Int? = null
    private var _nextId: Long? = null
    private var _globalId: Long? = null
    private var _synced: Boolean? = null

    fun putId(id: Long): BudgetBuilder {
        _id = id
        return this
    }

    fun putName(name: String): BudgetBuilder {
        _name = name
        return this
    }

    fun putLimit(limit: Double): BudgetBuilder {
        _limit = limit
        return this
    }

    fun putFrom(from: Date): BudgetBuilder {
        _from = from
        return this
    }

    fun putTo(to: Date): BudgetBuilder {
        _to = to
        return this
    }

    fun putType(type: Int): BudgetBuilder {
        _type = type
        return this
    }

    fun putNextId(nextId: Long?): BudgetBuilder {
        _nextId = nextId
        return this
    }

    fun putGlobalId(id: Long?): BudgetBuilder {
        _globalId = id
        return this
    }

    fun putSynced(s: Boolean?): BudgetBuilder {
        _synced = s
        return this
    }

    fun putDependency(type: Int, refId: String) {
        dependencies.add(Pair(type, refId))
    }

    fun create(): Observable<BudgetPlan> {
        return budgetService.createBudget(this)
    }

    fun createInternal(): BudgetPlan {
        return budgetService.createBudgetInternal(this)
    }

    fun update(): Observable<BudgetPlan> {
        return budgetService.updateBudget(this)
    }

    fun updateInternal(): BudgetPlan {
        return budgetService.updateBudgetInternal(this)
    }

}
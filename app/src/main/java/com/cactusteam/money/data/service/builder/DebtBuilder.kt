package com.cactusteam.money.data.service.builder

import com.cactusteam.money.data.dao.Debt
import com.cactusteam.money.data.service.DebtService
import rx.Observable
import java.util.*

/**
 * @author vpotapenko
 */
class DebtBuilder(private val debtService: DebtService) {

    val id: Long? get() = _id
    val name: String? get() = _name
    val phone: String? get() = _phone
    val accountId: Long? get() = _accountId
    val till: Date? get() = _till
    val start: Date? get() = _start
    val contactId: Long? get() = _contactId
    val finished: Boolean? get() = _finished
    val globalId: Long? get() = _globalId
    val synced: Boolean? get() = _synced

    private var _id: Long? = null
    private var _name: String? = null
    private var _phone: String? = null
    private var _accountId: Long? = null
    private var _till: Date? = null
    private var _start: Date? = null
    private var _contactId: Long? = null
    private var _finished: Boolean? = null
    private var _globalId: Long? = null
    private var _synced: Boolean? = null

    fun putId(id: Long): DebtBuilder {
        _id = id
        return this
    }

    fun putName(name: String): DebtBuilder {
        _name = name
        return this
    }

    fun putPhone(phone: String?): DebtBuilder {
        _phone = phone
        return this
    }

    fun putAccountId(accountId: Long): DebtBuilder {
        _accountId = accountId
        return this
    }

    fun putTill(date: Date?): DebtBuilder {
        _till = date
        return this
    }

    fun putStart(date: Date?): DebtBuilder {
        _start = date
        return this
    }

    fun putContactId(contactId: Long?): DebtBuilder {
        _contactId = contactId
        return this
    }

    fun putFinished(b: Boolean): DebtBuilder {
        _finished = b
        return this
    }

    fun putGlobalId(id: Long?): DebtBuilder {
        _globalId = id
        return this
    }

    fun putSynced(s: Boolean?): DebtBuilder {
        _synced = s
        return this
    }

    fun update(): Observable<Debt> {
        return debtService.updateDebt(this)
    }

    fun updateInternal(): Debt {
        return debtService.updateDebtInternal(this)
    }

    fun create(type: Int, amount: Double): Observable<Debt> {
        return debtService.createDebt(this, type, amount)
    }

    fun createInternal(): Debt {
        return debtService.createDebtInternal(this)
    }
}
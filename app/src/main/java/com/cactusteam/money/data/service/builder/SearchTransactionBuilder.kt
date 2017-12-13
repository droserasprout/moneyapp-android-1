package com.cactusteam.money.data.service.builder

import com.cactusteam.money.data.dao.Transaction
import com.cactusteam.money.data.filter.ITransactionFilter
import com.cactusteam.money.data.service.TransactionService
import rx.Observable
import java.util.*

/**
 * @author vpotapenko
 */
class SearchTransactionBuilder(private val service: TransactionService) {

    val convertToMainCurrency: Boolean
        get() = _convertToMainCurrency
    val max: Int?
        get() = _max
    val accountId: Long?
        get() = _accountId
    val categoryId: Long?
        get() = _categoryId
    val subcategoryId: Long?
        get() = _subcategoryId
    val from: Date?
        get() = _from
    val to: Date?
        get() = _to
    val ref: String?
        get() = _ref
    val status: Int
        get() = _status
    val notStatus: Int?
        get() = _notStatus
    val filter: ITransactionFilter?
        get() = _filter

    private var _convertToMainCurrency = false

    private var _max: Int? = null

    private var _accountId: Long? = null
    private var _categoryId: Long? = null
    private var _subcategoryId: Long? = null

    private var _from: Date? = null
    private var _to: Date? = null

    private var _ref: String? = null
    private var _status:Int = Transaction.STATUS_COMPLETED
    private var _notStatus:Int? = null

    private var _filter: ITransactionFilter? = null

    fun list(): Observable<List<Transaction>> {
        return service.listTransaction(this)
    }

    fun listInternal(): List<Transaction> {
        return service.listTransactionInternal(this)
    }

    fun countInternal(): Long {
        return service.countTransactionInternal(this)
    }

    fun putConvertToMain(b: Boolean): SearchTransactionBuilder {
        _convertToMainCurrency = b
        return this
    }

    fun putMax(m: Int): SearchTransactionBuilder {
        _max = m
        return this
    }

    fun putAccountId(id: Long): SearchTransactionBuilder {
        _accountId = id
        return this
    }

    fun putCategoryId(id: Long): SearchTransactionBuilder {
        _categoryId = id
        return this
    }

    fun putSubcategoryId(id: Long): SearchTransactionBuilder {
        _subcategoryId = id
        return this
    }

    fun putFrom(d: Date): SearchTransactionBuilder {
        _from = d
        return this
    }

    fun putTo(d: Date): SearchTransactionBuilder {
        _to = d
        return this
    }

    fun putRef(r: String): SearchTransactionBuilder {
        _ref = r
        return this
    }

    fun putStatus(s: Int): SearchTransactionBuilder {
        _status = s
        return this
    }

    fun putNotStatus(s: Int): SearchTransactionBuilder {
        _notStatus = s
        return this
    }

    fun putTransactionFilter(f: ITransactionFilter): SearchTransactionBuilder {
        _filter = f
        return this
    }
}
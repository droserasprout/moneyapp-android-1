package com.cactusteam.money.data.service.builder

import com.cactusteam.money.data.dao.CurrencyRate
import com.cactusteam.money.data.dao.Transaction
import java.util.*

/**
 * @author vpotapenko
 */
@Suppress("UNCHECKED_CAST")
abstract class BaseTransactionBuilder<out T> {

    val tags: MutableList<String>
        get() = _tags

    val id: Long?
        get() = _id
    val name: String?
        get() = _name
    val type: Int
        get() = _type
    val date: Date?
        get() = _date
    val sourceAccountId: Long
        get() = _sourceAccountId
    val amount: Double
        get() = _amount
    val categoryId: Long?
        get() = _categoryId
    val subcategoryId: Long?
        get() = _subcategoryId
    val destAccountId: Long?
        get() = _destAccountId
    val destAmount: Double?
        get() = _destAmount
    val comment: String?
        get() = _comment
    val rate: CurrencyRate?
        get() = _rate
    val ref: String?
        get() = _ref
    val status: Int
        get() = _status
    val globalId: Long?
        get() = _globalId
    val synced: Boolean?
        get() = _synced

    protected val _tags: MutableList<String> = mutableListOf()

    protected var _name: String? = null
    protected var _id: Long? = null
    protected var _type: Int = 0
    protected var _date: Date? = null
    protected var _sourceAccountId: Long = 0
    protected var _amount: Double = .0
    protected var _categoryId: Long? = null
    protected var _subcategoryId: Long? = null
    protected var _destAccountId: Long? = null
    protected var _destAmount: Double? = null
    protected var _comment: String? = null
    protected var _rate: CurrencyRate? = null
    protected var _ref: String? = null
    protected var _status: Int = Transaction.STATUS_COMPLETED
    protected var _globalId: Long? = null
    protected var _synced: Boolean? = null

    fun putName(name: String): T {
        _name = name
        return this as T
    }

    fun putType(t: Int): T {
        _type = t
        return this as T
    }

    fun putTag(t: String): T {
        _tags.add(t)
        return this as T
    }

    fun putDate(d: Date): T {
        _date = d
        return this as T
    }

    fun putSourceAccountId(id: Long): T {
        _sourceAccountId = id
        return this as T
    }

    fun putAmount(a: Double): T {
        _amount = a
        return this as T
    }

    fun putCategoryId(id: Long?): T {
        _categoryId = id
        return this as T
    }

    fun putSubcategoryId(id: Long?): T {
        _subcategoryId = id
        return this as T
    }

    fun putDestAccountId(id: Long?): T {
        _destAccountId = id
        return this as T
    }

    fun putDestAmount(a: Double?): T {
        _destAmount = a
        return this as T
    }

    fun putComment(c: String?): T {
        _comment = c
        return this as T
    }

    fun putRate(r: CurrencyRate?): T {
        _rate = r
        return this as T
    }

    fun putRef(r: String?): T {
        _ref = r
        return this as T
    }

    fun putStatus(s: Int): T {
        _status = s
        return this as T
    }

    fun putGlobalId(id: Long?): T {
        _globalId = id
        return this as T
    }

    fun putSynced(s: Boolean?): T {
        _synced = s
        return this as T
    }

    fun putId(id: Long): T {
        _id = id
        return this as T
    }
}
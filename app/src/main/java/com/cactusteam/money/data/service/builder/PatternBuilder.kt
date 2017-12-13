package com.cactusteam.money.data.service.builder

import com.cactusteam.money.data.dao.TransactionPattern
import com.cactusteam.money.data.service.PatternService
import rx.Observable

/**
 * @author vpotapenko
 */
class PatternBuilder(val parent: PatternService) : BaseTransactionBuilder<PatternBuilder>() {

    fun create(): Observable<TransactionPattern> {
        return parent.createPattern(this)
    }

    fun createInternal(): TransactionPattern {
        return parent.createPatternInternal(this)
    }

    fun update(): Observable<TransactionPattern> {
        return parent.updatePattern(this)
    }

    fun updateInternal(): TransactionPattern {
        return parent.updatePatternInternal(this)
    }
}
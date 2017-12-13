package com.cactusteam.money.data.service

import com.cactusteam.money.data.DataManager
import com.cactusteam.money.data.currency.RateLoader
import com.cactusteam.money.data.dao.CurrencyRate
import com.cactusteam.money.data.dao.CurrencyRateDao
import org.apache.commons.lang3.time.DateUtils
import java.util.*

/**
 * @author vpotapenko
 */
abstract class CurrencyInternalService(dataManager: DataManager) : BaseService(dataManager) {

    fun deleteRateInternal(rateId: Long) {
        dataManager.daoSession.currencyRateDao.deleteByKey(rateId)
    }

    fun updateRateInternal(sourceCode: String, destCode: String, rate: Double): CurrencyRate {
        val daoSession = dataManager.daoSession
        var list = daoSession.currencyRateDao.queryBuilder()
                .where(CurrencyRateDao.Properties.SourceCurrencyCode.eq(sourceCode), CurrencyRateDao.Properties.DestCurrencyCode.eq(destCode))
                .list()
        for (r in list) {
            daoSession.delete(r)
        }

        // inverse rate
        list = daoSession.currencyRateDao.queryBuilder()
                .where(CurrencyRateDao.Properties.SourceCurrencyCode.eq(destCode), CurrencyRateDao.Properties.DestCurrencyCode.eq(sourceCode))
                .list()
        for (r in list) {
            daoSession.delete(r)
        }

        val r = CurrencyRate()
        r.date = Date()
        r.sourceCurrencyCode = sourceCode
        r.destCurrencyCode = destCode
        r.rate = rate
        daoSession.insert(r)

        return r
    }

    fun getRatesInternal(): List<CurrencyRate> {
        val daoSession = dataManager.daoSession
        return daoSession.currencyRateDao.queryBuilder()
                .orderAsc(CurrencyRateDao.Properties.SourceCurrencyCode, CurrencyRateDao.Properties.DestCurrencyCode)
                .list()
    }

    fun getRateInternal(currencyCode1: String, currencyCode2: String): CurrencyRate? {
        var currencyRate: CurrencyRate? = findExistingCurrencyRate(currencyCode1, currencyCode2)
        if (currencyRate == null || !DateUtils.isSameDay(Date(), currencyRate.date)) {
            val loadedRate = loadRate(currencyCode1, currencyCode2)
            currencyRate = loadedRate ?: currencyRate
        }
        return currencyRate
    }

    private fun loadRate(currencyCode1: String, currencyCode2: String): CurrencyRate? {
        val rateLoader = RateLoader()
        rateLoader.initialize(currencyCode1, currencyCode2)
        rateLoader.load()
        if (rateLoader.rate == null) return null

        val currencyRate = CurrencyRate()
        currencyRate.sourceCurrencyCode = rateLoader.code1
        currencyRate.destCurrencyCode = rateLoader.code2
        currencyRate.rate = rateLoader.rate!!

        updateRate(currencyRate)

        return currencyRate
    }

    private fun updateRate(rate: CurrencyRate) {
        val daoSession = dataManager.daoSession
        val list = daoSession.currencyRateDao.queryBuilder().where(
                CurrencyRateDao.Properties.SourceCurrencyCode.eq(rate.sourceCurrencyCode),
                CurrencyRateDao.Properties.DestCurrencyCode.eq(rate.destCurrencyCode))
                .orderDesc(CurrencyRateDao.Properties.Date)
                .limit(1)
                .list()

        if (list.isEmpty()) {
            rate.date = Date()
            daoSession.insert(rate)
        } else {
            val existingRate = list.get(0)
            existingRate.date = Date()
            existingRate.rate = rate.rate

            daoSession.update(existingRate)
        }
    }

    private fun findExistingCurrencyRate(currencyCode1: String, currencyCode2: String): CurrencyRate? {
        val rate1 = findRate(currencyCode1, currencyCode2)
        val rate2 = findRate(currencyCode2, currencyCode1)

        if (rate1 == null) {
            return rate2
        } else if (rate2 == null) {
            return rate1
        } else {
            return if (rate1.date.after(rate2.date)) rate1 else rate2
        }
    }

    private fun findRate(sourceCode: String, destCode: String): CurrencyRate? {
        val daoSession = dataManager.daoSession
        val list = daoSession.currencyRateDao.queryBuilder()
                .where(CurrencyRateDao.Properties.SourceCurrencyCode.eq(sourceCode),
                        CurrencyRateDao.Properties.DestCurrencyCode.eq(destCode))
                .orderDesc(CurrencyRateDao.Properties.Date)
                .limit(1)
                .list()
        return if (list.isEmpty()) null else list[0]
    }
}
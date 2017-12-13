package com.cactusteam.money.data.service

import com.cactusteam.money.data.DataManager
import com.cactusteam.money.data.dao.*
import com.cactusteam.money.data.service.builder.PatternBuilder
import com.cactusteam.money.sync.SyncConstants
import java.util.*

/**
 * @author vpotapenko
 */
abstract class PatternInternalService(dataManager: DataManager) : BaseService(dataManager) {

    fun getPatternsInternal(): List<TransactionPattern> {
        return dataManager.daoSession.transactionPatternDao
                .queryBuilder().orderAsc(TransactionPatternDao.Properties.Name).list()
    }

    fun getPatternInternal(id: Long): TransactionPattern {
        return dataManager.daoSession.transactionPatternDao.load(id)
    }

    fun deletePatternInternal(id: Long) {
        val daoSession = dataManager.daoSession

        val pattern = daoSession.transactionPatternDao.load(id)
        for (tag in pattern.tags) {
            daoSession.delete(tag)
        }
        if (pattern.globalId != null) {
            val trash = Trash()
            trash.type = SyncConstants.PATTERN_TYPE
            trash.globalId = pattern.globalId
            daoSession.insert(trash)
        }
        daoSession.delete(pattern)
    }

    fun createPatternInternal(builder: PatternBuilder): TransactionPattern {
        val pattern = TransactionPattern()
        pattern.type = builder.type
        fillPattern(builder, pattern)
        dataManager.daoSession.insert(pattern)

        createTags(pattern, builder.tags)

        return pattern
    }

    fun updatePatternInternal(builder: PatternBuilder): TransactionPattern {
        val daoSession = dataManager.daoSession
        val pattern = daoSession.transactionPatternDao.load(builder.id)
        if (builder.globalId == null && pattern.globalId != null) {
            builder.putGlobalId(pattern.globalId).putSynced(false)
        }

        fillPattern(builder, pattern)
        dataManager.daoSession.update(pattern)

        for (tag in pattern.tags) {
            val name = tag.tag.name
            if (builder.tags.contains(name)) {
                builder.tags.remove(name)
            } else {
                daoSession.delete(tag)
            }
        }
        createTags(pattern, builder.tags)

        pattern.resetTags()
        pattern.tags

        return pattern
    }

    private fun fillPattern(builder: PatternBuilder, pattern: TransactionPattern) {
        pattern.globalId = builder.globalId
        pattern.synced = builder.synced
        pattern.name = builder.name
        pattern.sourceAccountId = builder.sourceAccountId
        pattern.amount = builder.amount
        pattern.categoryId = builder.categoryId
        pattern.subcategoryId = builder.subcategoryId
        pattern.destAccountId = builder.destAccountId
        pattern.destAmount = builder.destAmount
        pattern.comment = builder.comment
    }

    fun createPatternFromTransactionInternal(transactionId: Long, name: String): TransactionPattern {
        val daoSession = dataManager.daoSession
        val source = daoSession.transactionDao.load(transactionId)

        val pattern = TransactionPattern()
        pattern.name = name
        pattern.type = source.type
        pattern.comment = source.comment
        pattern.sourceAccountId = source.sourceAccountId
        pattern.amount = source.amount
        pattern.categoryId = source.categoryId
        pattern.subcategoryId = source.subcategoryId
        pattern.destAccountId = source.destAccountId
        pattern.destAmount = source.destAmount

        daoSession.insert(pattern)

        for (tag in source.tags) {
            val newTag = PatternTag()
            newTag.tagId = tag.tagId
            newTag.patternId = pattern.id

            daoSession.insert(newTag)
        }

        return pattern
    }

    protected fun createTags(pattern: TransactionPattern, tags: List<String>) {
        for (tag in tags) {
            createPatternTag(pattern, tag)
        }
    }

    private fun createPatternTag(pattern: TransactionPattern, tagName: String) {
        val daoSession = dataManager.daoSession
        val list = daoSession.tagDao.queryBuilder().where(TagDao.Properties.Name.eq(tagName)).limit(1).list()

        val tag: Tag
        if (list.isEmpty()) {
            tag = Tag()
            tag.name = tagName
            tag.updated = Date()

            daoSession.insert(tag)
        } else {
            tag = list[0]
            tag.updated = Date()

            daoSession.update(tag)
        }

        val patternTag = PatternTag()
        patternTag.tag = tag
        patternTag.patternId = pattern.id

        daoSession.insert(patternTag)
    }
}
package com.cactusteam.money.data.service

import com.cactusteam.money.data.DataManager
import com.cactusteam.money.data.model.TagInfo
import com.cactusteam.money.data.dao.*
import com.cactusteam.money.data.filter.TypeTransactionFilter
import com.cactusteam.money.data.service.TagService.Companion.WITHOUT_TAGS_ID
import java.util.*

/**
 * @author vpotapenko
 */
abstract class TagInternalService(dataManager: DataManager) : BaseService(dataManager) {

    fun getTagAmountsInternal(type: Int): List<TagInfo> {
        val current = getApplication().period.current
        val transactions = dataManager.transactionService
                .newListTransactionsBuilder()
                .putFrom(current.first)
                .putTo(current.second)
                .putTransactionFilter(TypeTransactionFilter(type))
                .putConvertToMain(true)
                .listInternal()
        val tagAmounts: MutableMap<Long, TagInfo> = mutableMapOf()
        for (t in transactions) {
            val tags = t.tags
            if (tags.isEmpty()) {
                handleTagAmount(WITHOUT_TAGS_ID, null, t.amountInMainCurrency, tagAmounts)
            } else {
                for (tag in tags) {
                    handleTagAmount(tag.tagId, tag.tag.name, t.amountInMainCurrency, tagAmounts)
                }
            }
        }

        val tags = dataManager.daoSession.tagDao.loadAll()
        for (tag in tags) {
            if (!tagAmounts.containsKey(tag.id)) {
                val tagInfo = TagInfo(tag.id, tag.name)
                tagAmounts.put(tag.id, tagInfo)
            }
        }

        return tagAmounts.values.toList()
    }

    fun updateTagInternal(oldName: String, newName: String) {
        val daoSession = dataManager.daoSession

        val list = daoSession.tagDao.queryBuilder().where(TagDao.Properties.Name.eq(newName)).list()
        val newTag = if (list.isEmpty()) {
            val tag = Tag()
            tag.name = newName
            tag.updated = Date()
            daoSession.insert(tag)
            tag
        } else {
            list[0]
        }

        daoSession.runInTx { updateTransactions(newTag, oldName, newName) }
        daoSession.runInTx { updatePatterns(newTag, oldName, newName) }
        removeTag(oldName)
    }

    fun deleteTagInternal(tagName: String) {
        removeTransactionTags(tagName)
        removePatternTags(tagName)
        removeTag(tagName)
    }

    fun getTagsInternal(): List<Tag> {
        val daoSession = dataManager.daoSession

        return daoSession.tagDao.queryBuilder().orderDesc(TagDao.Properties.Updated).list()
    }

    private fun removePatternTags(tagName: String) {
        val daoSession = dataManager.daoSession
        val patterns = daoSession.transactionPatternDao.loadAll()
        val tags = ArrayList<PatternTag>()
        for (p in patterns) {
            findTags(p.tags, tags, tagName)
            if (tags.isEmpty()) continue

            for (tag in tags) {
                daoSession.delete(tag)
            }
            patternUpdated(p)
        }
    }

    private fun removeTransactionTags(tagName: String) {
        val daoSession = dataManager.daoSession
        val transactions = daoSession.transactionDao.loadAll()
        val tags = ArrayList<TransactionTag>()
        for (t in transactions) {
            findTags(t.tags, tags, tagName)
            if (tags.isEmpty()) continue

            for (tag in tags) {
                daoSession.delete(tag)
            }
            transactionUpdated(t)
        }
    }

    private fun <T : ITagContainer> findTags(tags: List<T>, result: MutableList<T>, tagName: String) {
        result.clear()
        tags.filterTo(result) { tagName == it.tag.name }
    }

    private fun transactionUpdated(t: Transaction) {
        if (t.globalId != null) {
            t.synced = false
            dataManager.daoSession.update(t)
        }
        t.resetTags()
        t.tags
    }

    private fun patternUpdated(p: TransactionPattern) {
        if (p.globalId != null) {
            p.synced = false
            dataManager.daoSession.update(p)
        }

        p.resetTags()
        p.tags
    }

    private fun removeTag(tagName: String) {
        val daoSession = dataManager.daoSession
        val tags = daoSession.tagDao.queryBuilder().where(TagDao.Properties.Name.eq(tagName)).list()
        for (tag in tags) {
            daoSession.delete(tag)
        }
    }

    private fun updateTransactions(newTag: Tag, oldName: String, newName: String) {
        val daoSession = dataManager.daoSession
        val transactions = daoSession.transactionDao.loadAll()
        val tags = ArrayList<TransactionTag>()
        for (t in transactions) {
            findTags(t.tags, tags, oldName)
            if (tags.isEmpty()) continue

            for (tag in tags) {
                daoSession.delete(tag)
            }

            findTags(t.tags, tags, newName)
            if (tags.isEmpty()) {
                val tag = TransactionTag()
                tag.tagId = newTag.id!!
                tag.transactionId = t.id!!
                daoSession.insert(tag)
            }

            transactionUpdated(t)
        }
    }

    private fun updatePatterns(newTag: Tag, oldName: String, newName: String) {
        val daoSession = dataManager.daoSession
        val patterns = daoSession.transactionPatternDao.loadAll()
        val tags = ArrayList<PatternTag>()
        for (p in patterns) {
            findTags(p.tags, tags, oldName)
            if (tags.isEmpty()) continue

            for (tag in tags) {
                daoSession.delete(tag)
            }

            findTags(p.tags, tags, newName)
            if (tags.isEmpty()) {
                val tag = PatternTag()
                tag.tagId = newTag.id!!
                tag.patternId = p.id!!
                daoSession.insert(tag)
            }

            patternUpdated(p)
        }
    }

    private fun handleTagAmount(tagId: Long, tagName: String?, amount: Double, tagAmounts: MutableMap<Long, TagInfo>) {
        var tagInfo: TagInfo? = tagAmounts[tagId]
        if (tagInfo == null) {
            tagInfo = TagInfo(tagId, tagName)
            tagAmounts.put(tagId, tagInfo)
        }
        tagInfo.amount += amount
    }
}
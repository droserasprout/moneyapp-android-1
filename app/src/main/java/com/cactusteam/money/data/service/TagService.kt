package com.cactusteam.money.data.service

import com.cactusteam.money.data.DataManager
import com.cactusteam.money.data.model.TagInfo
import com.cactusteam.money.data.dao.Tag
import rx.Observable
import rx.lang.kotlin.observable

/**
 * @author vpotapenko
 */
class TagService(dataManager: DataManager) : TagInternalService(dataManager) {

    fun getTagAmounts(type: Int): Observable<List<TagInfo>> {
        val o = observable<List<TagInfo>> { s ->
            try {
                s.onNext(getTagAmountsInternal(type))
                s.onCompleted()
            } catch (e: Exception) {
                e.printStackTrace()
                s.onError(e)
            }
        }
        return prepareIOObservable(o)
    }

    fun updateTag(oldName: String, newName: String): Observable<Unit> {
        val o = observable<Unit> { s ->
            try {
                updateTagInternal(oldName, newName)
                s.onCompleted()
            } catch (e: Exception) {
                e.printStackTrace()
                s.onError(e)
            }
        }
        return prepareIOObservable(o)
    }

    fun deleteTag(tagName: String): Observable<Unit> {
        val o = observable<Unit> { s ->
            try {
                deleteTagInternal(tagName)
                s.onCompleted()
            } catch (e: Exception) {
                e.printStackTrace()
                s.onError(e)
            }
        }
        return prepareIOObservable(o)
    }

    fun getTags(): Observable<List<Tag>> {
        val o = observable<List<Tag>> { s ->
            try {
                s.onNext(getTagsInternal())
                s.onCompleted()
            } catch (e: Exception) {
                e.printStackTrace()
                s.onError(e)
            }
        }
        return prepareIOObservable(o)
    }

    companion object {

        val WITHOUT_TAGS_ID = -1L
    }
}
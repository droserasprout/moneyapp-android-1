package com.cactusteam.money.data.service

import com.cactusteam.money.app.MoneyApp
import com.cactusteam.money.data.DataManager
import com.cactusteam.money.data.dao.Category
import com.cactusteam.money.data.dao.CategoryDao
import rx.Observable
import rx.Subscriber
import rx.android.schedulers.AndroidSchedulers
import rx.lang.kotlin.observable
import rx.schedulers.Schedulers

/**
 * @author vpotapenko
 */
abstract class BaseService(val dataManager: DataManager) {

    protected fun <T> wrap(f: (s: Subscriber<in T>) -> Unit): Observable<T> {
        val o = observable<T> { s ->
            try {
                f(s)
                s.onCompleted()
            } catch (e: Exception) {
                e.printStackTrace()
                s.onError(e)
            }
        }
        return prepareIOObservable(o)
    }

    fun <T> prepareIOObservable(o: Observable<T>): Observable<T> {
        return o.observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
    }

    fun getApplication(): MoneyApp {
        return MoneyApp.instance
    }

    protected fun findCategoryByName(type: Int, name: String, newIcon: String, readOnly: Boolean = false): Long {
        val daoSession = dataManager.daoSession
        var categories = daoSession.categoryDao.queryBuilder().where(
                CategoryDao.Properties.Type.eq(type),
                CategoryDao.Properties.Name.eq(name),
                CategoryDao.Properties.Deleted.eq(false)).limit(1).list()
        if (!categories.isEmpty()) return categories[0].id

        if (readOnly) return -1

        categories = daoSession.categoryDao.queryBuilder().where(
                CategoryDao.Properties.Type.eq(type),
                CategoryDao.Properties.Name.eq(name)).limit(1).list()
        if (!categories.isEmpty()) {
            val category = categories[0]
            category.deleted = false
            daoSession.update(category)

            return category.id
        } else {
            val category = Category()
            category.name = name
            category.type = type
            category.icon = newIcon

            daoSession.insert(category)

            return category.id
        }
    }
}
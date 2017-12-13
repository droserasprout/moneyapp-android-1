package com.cactusteam.money.data.service

import com.cactusteam.money.data.model.CategoryAmounts
import com.cactusteam.money.data.model.CategoryPeriodData
import com.cactusteam.money.data.DataManager
import com.cactusteam.money.data.dao.Category
import com.cactusteam.money.data.dao.Subcategory
import rx.Observable
import rx.lang.kotlin.observable

/**
 * @author vpotapenko
 */
class CategoryService(dataManager: DataManager) : CategoryInternalService(dataManager) {

    fun updateSubcategory(id: Long,
                          name: String,
                          globalId: Long? = null,
                          synced: Boolean? = null): Observable<Subcategory> {
        val o = observable<Subcategory> { s ->
            try {
                s.onNext(updateSubcategoryInternal(id, name, globalId, synced))
                s.onCompleted()
            } catch (e: Exception) {
                e.printStackTrace()
                s.onError(e)
            }
        }
        return prepareIOObservable(o)
    }

    fun createSubcategory(parentId: Long,
                          name: String,
                          globalId: Long? = null,
                          synced: Boolean? = null): Observable<Subcategory> {
        val o = observable<Subcategory> { s ->
            try {
                s.onNext(createSubcategoryInternal(parentId, name, globalId, synced))
                s.onCompleted()
            } catch (e: Exception) {
                e.printStackTrace()
                s.onError(e)
            }
        }
        return prepareIOObservable(o)
    }

    fun getCategoryAmounts(): Observable<CategoryAmounts> {
        val o = observable<CategoryAmounts> { s ->
            try {
                s.onNext(getCategoryAmountsInternal())
                s.onCompleted()
            } catch (e: Exception) {
                e.printStackTrace()
                s.onError(e)
            }
        }
        return prepareIOObservable(o)
    }

    fun getSubcategory(subcategoryId: Long): Observable<Subcategory> {
        val o = observable<Subcategory> { s ->
            try {
                s.onNext(getSubcategoryInternal(subcategoryId))
                s.onCompleted()
            } catch (e: Exception) {
                e.printStackTrace()
                s.onError(e)
            }
        }
        return prepareIOObservable(o)
    }

    fun deleteSubcategory(subcategoryId: Long): Observable<Unit> {
        val o = observable<Unit> { s ->
            try {
                deleteSubcategoryInternal(subcategoryId)
                s.onCompleted()
            } catch (e: Exception) {
                e.printStackTrace()
                s.onError(e)
            }
        }
        return prepareIOObservable(o)
    }

    fun restoreSubcategory(subcategoryId: Long): Observable<Subcategory> {
        val o = observable<Subcategory> { s ->
            try {
                s.onNext(restoreSubcategoryInternal(subcategoryId))
                s.onCompleted()
            } catch (e: Exception) {
                e.printStackTrace()
                s.onError(e)
            }
        }
        return prepareIOObservable(o)
    }

    fun convertToCategory(subcategoryId: Long): Observable<Category> {
        val o = observable<Category> { s ->
            try {
                s.onNext(convertToCategoryInternal(subcategoryId))
                s.onCompleted()
            } catch (e: Exception) {
                e.printStackTrace()
                s.onError(e)
            }
        }
        return prepareIOObservable(o)
    }

    fun updateCategoriesOrder(orders: Map<Long, Int>): Observable<Unit> {
        val o = observable<Unit> { s ->
            try {
                updateCategoriesOrderInternal(orders)
                s.onCompleted()
            } catch (e: Exception) {
                e.printStackTrace()
                s.onError(e)
            }
        }
        return prepareIOObservable(o)
    }

    fun convertToSubcategory(id: Long, parentId: Long): Observable<Subcategory> {
        val o = observable<Subcategory> { s ->
            try {
                s.onNext(convertToSubcategoryInternal(id, parentId))
                s.onCompleted()
            } catch (e: Exception) {
                e.printStackTrace()
                s.onError(e)
            }
        }
        return prepareIOObservable(o)
    }

    fun createCategory(type: Int, name: String, icon: String?): Observable<Category> {
        val o = observable<Category> { s ->
            try {
                s.onNext(createCategoryInternal(type, name, icon))
                s.onCompleted()
            } catch (e: Exception) {
                e.printStackTrace()
                s.onError(e)
            }
        }
        return prepareIOObservable(o)
    }

    fun updateCategory(id: Long, name: String, icon: String?): Observable<Category> {
        val o = observable<Category> { s ->
            try {
                s.onNext(updateCategoryInternal(id, name, icon))
                s.onCompleted()
            } catch (e: Exception) {
                e.printStackTrace()
                s.onError(e)
            }
        }
        return prepareIOObservable(o)
    }

    fun deleteCategory(id: Long): Observable<Unit> {
        val o = observable<Unit> { s ->
            try {
                deleteCategoryInternal(id)
                s.onCompleted()
            } catch (e: Exception) {
                e.printStackTrace()
                s.onError(e)
            }
        }
        return prepareIOObservable(o)
    }

    fun restoreCategory(id: Long): Observable<Category> {
        val o = observable<Category> { s ->
            try {
                s.onNext(restoreCategoryInternal(id))
                s.onCompleted()
            } catch (e: Exception) {
                e.printStackTrace()
                s.onError(e)
            }
        }
        return prepareIOObservable(o)
    }

    fun getCategory(id: Long): Observable<Category> {
        val o = observable<Category> { s ->
            try {
                s.onNext(getCategoryInternal(id))
                s.onCompleted()
            } catch (e: Exception) {
                e.printStackTrace()
                s.onError(e)
            }
        }
        return prepareIOObservable(o)
    }

    fun getCategoryPeriodsData(id: Long, size: Int = 3, addAll: Boolean = false): Observable<List<CategoryPeriodData>> {
        val o = observable<List<CategoryPeriodData>> { s ->
            try {
                s.onNext(getCategoryPeriodsDataInternal(id, size, addAll))
                s.onCompleted()
            } catch (e: Exception) {
                e.printStackTrace()
                s.onError(e)
            }
        }
        return prepareIOObservable(o)
    }

    fun getCategories(type: Int, includeDeleted: Boolean = false): Observable<List<Category>> {
        val o = observable<List<Category>> { s ->
            try {
                s.onNext(getCategoriesInternal(type, includeDeleted))
                s.onCompleted()
            } catch (e: Exception) {
                e.printStackTrace()
                s.onError(e)
            }
        }
        return prepareIOObservable(o)
    }

    fun getFrequentlyCategories(type: Int): Observable<List<Category>?> {
        val o = observable<List<Category>?> { s ->
            try {
                s.onNext(getFrequentlyCategoriesInternal(type))
                s.onCompleted()
            } catch (e: Exception) {
                e.printStackTrace()
                s.onError(e)
            }
        }
        return prepareIOObservable(o)
    }
}
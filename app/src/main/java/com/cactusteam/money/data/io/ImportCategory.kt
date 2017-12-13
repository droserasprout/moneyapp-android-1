package com.cactusteam.money.data.io

import android.support.v4.util.ArrayMap

/**
 * @author vpotapenko
 */
class ImportCategory(var type: Int, var name: String, var strategy: CategoryImportStrategy) {

    private var categoryId: Long? = null

    var subcategoryMap: MutableMap<String, ImportSubcategory> = ArrayMap()

    fun getCategoryId(): Long? {
        if (categoryId == null) {
            categoryId = strategy.apply(type)
        }
        return categoryId
    }

    fun predictCategoryId(): Long? {
        return strategy.predictCategoryId()
    }
}

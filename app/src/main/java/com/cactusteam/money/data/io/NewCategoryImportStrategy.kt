package com.cactusteam.money.data.io

import com.cactusteam.money.app.MoneyApp

/**
 * @author vpotapenko
 */
class NewCategoryImportStrategy(var name: String) : CategoryImportStrategy() {

    override fun apply(type: Int): Long {
        val newCategory = MoneyApp.instance.dataManager.categoryService
                .createCategoryInternal(type, name, null)
        return newCategory.id
    }

    override fun predictCategoryId(): Long? {
        return null
    }
}

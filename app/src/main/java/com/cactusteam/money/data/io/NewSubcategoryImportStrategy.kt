package com.cactusteam.money.data.io

import com.cactusteam.money.app.MoneyApp

/**
 * @author vpotapenko
 */
class NewSubcategoryImportStrategy : SubcategoryImportStrategy() {

    override fun apply(name: String, categoryId: Long): Long {
        val subcategory = MoneyApp.instance.dataManager.categoryService.createSubcategoryInternal(categoryId, name)
        return subcategory.id
    }
}

package com.cactusteam.money.data.io

import com.cactusteam.money.data.dao.Subcategory

/**
 * @author vpotapenko
 */
class ExistSubcategoryImportStrategy(private val subcategory: Subcategory?) : SubcategoryImportStrategy() {

    override fun apply(name: String, categoryId: Long): Long {
        return if (subcategory == null) -1 else subcategory.id
    }
}

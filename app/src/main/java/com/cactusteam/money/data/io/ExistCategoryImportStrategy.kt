package com.cactusteam.money.data.io

import com.cactusteam.money.data.dao.Category

/**
 * @author vpotapenko
 */
class ExistCategoryImportStrategy(var category: Category) : CategoryImportStrategy() {

    override fun apply(type: Int): Long {
        return category.id!!
    }

    override fun predictCategoryId(): Long? {
        return category.id
    }
}

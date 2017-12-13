package com.cactusteam.money.data.io

/**
 * @author vpotapenko
 */
class ImportSubcategory(var parent: ImportCategory, var name: String, var strategy: SubcategoryImportStrategy) {

    private var subcategoryId: Long? = null
    private var initialized: Boolean = false

    fun getSubcategoryId(): Long? {
        if (!initialized) {
            val result = strategy.apply(name, parent.getCategoryId()!!)
            subcategoryId = if (result < 0) null else result
            initialized = true
        }
        return subcategoryId
    }
}

package com.cactusteam.money.data.io

/**
 * @author vpotapenko
 */
abstract class CategoryImportStrategy {

    abstract fun apply(type: Int): Long

    abstract fun predictCategoryId(): Long?
}

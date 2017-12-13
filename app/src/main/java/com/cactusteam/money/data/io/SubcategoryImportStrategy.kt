package com.cactusteam.money.data.io

/**
 * @author vpotapenko
 */
abstract class SubcategoryImportStrategy {

    abstract fun apply(name: String, categoryId: Long): Long
}

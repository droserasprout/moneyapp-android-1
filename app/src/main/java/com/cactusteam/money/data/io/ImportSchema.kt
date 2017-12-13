package com.cactusteam.money.data.io

import android.support.v4.util.ArrayMap
import java.util.*

/**
 * @author vpotapenko
 */
class ImportSchema {

    var format: Int = 0

    var cellsNumber: Int = 0
    var linesCount: Int = 0
    var badLinesCount: Int = 0

    private val accounts = ArrayMap<String, ImportAccount>()
    private val categories = ArrayMap<String, ImportCategory>()

    internal fun findAccountByName(accountName: String): ImportAccount? {
        return accounts.values.firstOrNull { it.name == accountName }
    }

    internal fun getAccount(accountName: String, currencyCode: String?): ImportAccount? {
        val accountKey = getAccountKey(accountName, currencyCode)
        return accounts[accountKey]
    }

    fun getCategory(categoryName: String, expense: Boolean): ImportCategory? {
        val categoryKey = getCategoryKey(categoryName, expense)
        return categories[categoryKey]
    }

    internal fun putAccount(accountName: String, currencyCode: String?, account: ImportAccount) {
        accounts.put(getAccountKey(accountName, currencyCode), account)
    }

    internal fun putCategory(categoryName: String, expense: Boolean, category: ImportCategory) {
        categories.put(getCategoryKey(categoryName, expense), category)
    }

    private fun getAccountKey(accountName: String, currencyCode: String?): String {
        return if (currencyCode != null) accountName + " - " + currencyCode else accountName
    }

    private fun getCategoryKey(categoryName: String, expense: Boolean): String {
        return categoryName + if (expense) '-' else '+'
    }

    val allAccounts: List<ImportAccount>
        get() {
            val result = ArrayList(accounts.values)
            Collections.sort(result, ACCOUNT_COMPARATOR)

            return result
        }

    fun getAllCategories(type: Int): List<ImportCategory> {
        val result = categories.values.filter { it.type == type }
        Collections.sort(result, CATEGORY_COMPARATOR)

        return result
    }

    companion object {

        private val ACCOUNT_COMPARATOR = Comparator<ImportAccount> { lhs, rhs -> lhs.name.compareTo(rhs.name) }

        private val CATEGORY_COMPARATOR = Comparator<ImportCategory> { lhs, rhs -> lhs.name.compareTo(rhs.name) }
    }
}

package com.cactusteam.money.ui

import com.cactusteam.money.R

/**
 * @author vpotapenko
 */
enum class MainSection constructor(val titleResId: Int, val id: Int) {

    HOME(R.string.home_title, R.id.home),
    ACCOUNTS(R.string.accounts_title, R.id.accounts),
    CATEGORIES(R.string.categories_title, R.id.categories),
    TRANSACTIONS(R.string.transactions_title, R.id.transactions),
    BUDGET(R.string.budget_title, R.id.budget),
    DEBTS(R.string.debts_title, R.id.debts),
    REPORTS(R.string.reports_title, R.id.reports),
    SYNC(R.string.sync_title, R.id.sync),
    SETTINGS(R.string.settings_title, R.id.settings),
    TAGS(R.string.tags, R.id.tags),
    DONATION(R.string.donation_title, R.id.donation);


    companion object {

        fun find(name: String?): MainSection? {
            return values().firstOrNull { it.name == name }
        }
    }
}

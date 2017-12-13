package com.cactusteam.money.ui

import com.cactusteam.money.R

import org.apache.commons.lang3.time.DateUtils

/**
 * @author vpotapenko
 */
object UiConstants {
    val MONEY_APP_FOLDER_NAME = "MoneyApp"

    val DELETED_PATTERN = "<strike>%s</strike>"

    val DEFAULT_BLOCK_ORDER = "accounts,balance,patterns,budget,budgetSummary,balanceChart,expenseChart,incomeChart,transactions"
    val TOTAL_BALANCE_BLOCK = "balance"
    val PATTERNS_BLOCK = "patterns"
    val BUDGET_BLOCK = "budget"
    val TRANSACTIONS_BLOCK = "transactions"
    val ACCOUNTS_BLOCK = "accounts"
    val BALANCE_CHART_BLOCK = "balanceChart"
    val EXPENSE_CHART_BLOCK = "expenseChart"
    val INCOME_CHART_BLOCK = "incomeChart"
    val BUDGET_SUMMARY_BLOCK = "budgetSummary"
    val NOTES_BLOCK = "notes"

    val EXTRA_ID = "com.cactusteam.money.id"
    val EXTRA_NAME = "com.cactusteam.money.name"
    val EXTRA_COLOR = "com.cactusteam.money.color"
    val EXTRA_DELETED = "com.cactusteam.money.deleted"
    val EXTRA_TYPE = "com.cactusteam.money.type"
    val EXTRA_PARENT = "com.cactusteam.money.parent"
    val EXTRA_FILTER = "com.cactusteam.money.filter"
    val EXTRA_FILTER_DESCRIPTION = "com.cactusteam.money.filter.description"
    val EXTRA_ACCOUNT = "com.cactusteam.money.account"
    val EXTRA_CATEGORY = "com.cactusteam.money.category"
    val EXTRA_SUBCATEGORY = "com.cactusteam.money.subcategory"
    val EXTRA_MODE = "com.cactusteam.money.mode"
    val EXTRA_START = "com.cactusteam.money.start"
    val EXTRA_FINISH = "com.cactusteam.money.finish"
    val EXTRA_COPY = "com.cactusteam.money.copy"
    val EXTRA_DEST_ACCOUNT = "com.cactusteam.money.destAccount"
    val EXTRA_TIME = "com.cactusteam.money.time"
    val EXTRA_AMOUNT = "com.cactusteam.money.amount"
    val EXTRA_TAGS = "com.cactusteam.money.tags"
    val EXTRA_RATE = "com.cactusteam.money.rate"
    val EXTRA_CHANGES = "com.cactusteam.money.changes"
    val EXTRA_COMMENT = "com.cactusteam.money.comment"

    val EDIT_ACCOUNT_REQUEST_CODE = 1024
    val ACCOUNT_REQUEST_CODE = 1025
    val CATEGORY_REQUEST_CODE = 1026
    val EDIT_TRANSACTION_REQUEST_CODE = 1027
    val EDIT_CATEGORY_REQUEST_CODE = 1028
    val SUBCATEGORY_REQUEST_CODE = 1029
    val EDIT_BUDGET_PLAN_REQUEST_CODE = 1030
    val BUDGET_PLAN_REQUEST_CODE = 1031
    val EDIT_DEBT_REQUEST_CODE = 1032
    val DEBT_REQUEST_CODE = 1033
    val FILTERED_TRANSACTIONS_REQUEST_CODE = 1034
    val PATTERN_REQUEST_CODE = 1035
    val CATEGORIES_REPORT_REQUEST_CODE = 1036
    val PASSWORD_REQUEST_CODE = 1037
    val SUBCATEGORIES_REPORT_REQUEST_CODE = 1038
    val TAGS_REPORT_REQUEST_CODE = 1039
    val IMPORT_TRANSACTIONS_REQUEST_CODE = 1040
    val CONNECT_SYNC_REQUEST_CODE = 1041
    val DROPBOX_REQUEST_CODE = 1042
    val NEW_CATEGORY_REQUEST_CODE = 1043
    val NEW_ACCOUNT_REQUEST_CODE = 1044
    val CATEGORY_REPORT_REQUEST_CODE = 1045
    val SORTING_REQUEST_CODE = 1046
    val TAG_REQUEST_CODE = 1047
    val CONTACT_REQUEST_CODE = 1048
    val GROUP_INPUT_REQUEST_CODE = 1049
    val CALCULATOR_REQUEST_CODE = 1050
    val SOURCE_ACCOUNT_REQUEST_CODE = 1051
    val DEST_ACCOUNT_REQUEST_CODE = 1052
    val INCREASE_REQUEST_CODE = 1053
    val DECREASE_REQUEST_CODE = 1054
    val IAB_REQUEST_CODE = 1055

    val PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE_ON_SAVE: Byte = 10
    val PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE_ON_SEND: Byte = 11
    val PERMISSIONS_REQUEST_READ_CONTACTS: Byte = 12
    val PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE: Byte = 13

    val MAX_SHORT_TRANSACTIONS = 10
    val TOOLBAR_TRANSITION_DURATION = 500

    val LOGIN_PAUSE = DateUtils.MILLIS_PER_MINUTE

    val UI_COLORS = intArrayOf(
            R.color.color1,
            R.color.color2,
            R.color.color3,
            R.color.color4,
            R.color.color5,
            R.color.color6,
            R.color.color7,
            R.color.color8,
            R.color.color9,
            R.color.color10,
            R.color.color11,
            R.color.color12,
            R.color.color13,
            R.color.color14,
            R.color.color15,
            R.color.color16,
            R.color.color17,
            R.color.color18
    )
}


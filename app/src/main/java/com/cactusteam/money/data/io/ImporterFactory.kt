package com.cactusteam.money.data.io

/**
 * @author vpotapenko
 */
object ImporterFactory {

    val MONEY_APP_CSV = 0
    val FINANCISTO_CSV = 1
    val FINANCIUS_CSV = 2
    val MONEFY_CSV = 3
    val MY_MONEY_DB = 4
    val QIF = 5
    val TINKOFF = 6
    val VIZI_BUDGET_CSV = 7

    fun create(type: Int): IImporter {
        when (type) {
            MONEY_APP_CSV -> return MoneyAppCsvImporter()
            FINANCISTO_CSV -> return FinancistoImporter()
            VIZI_BUDGET_CSV -> return ViZiBudgetImporter()
            MY_MONEY_DB -> return MyMoneyDbImporter()
            MONEFY_CSV -> return MonefyCsvImporter()
            FINANCIUS_CSV -> return FinanciusCsvImporter()
            TINKOFF -> return TinkoffCsvImporter()
            QIF -> return QifImporter()
            else -> throw RuntimeException("Unsupported import format")
        }
    }
}

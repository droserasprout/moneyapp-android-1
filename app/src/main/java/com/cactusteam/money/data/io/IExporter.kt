package com.cactusteam.money.data.io

import com.cactusteam.money.data.dao.Transaction

import java.io.File

/**
 * @author vpotapenko
 */
interface IExporter {

    fun initialize(mainCurrency: String)

    fun export(t: Transaction)

    fun commit(exportFolder: File): String
}

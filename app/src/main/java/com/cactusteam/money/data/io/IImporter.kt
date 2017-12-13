package com.cactusteam.money.data.io

import com.cactusteam.money.data.DataManager

import java.io.File

/**
 * @author vpotapenko
 */
interface IImporter {

    fun analyse(sourceFile: File, listener: (progress: Int, max: Int) -> Unit, dataManager: DataManager): ImportSchema

    fun doImport(schema: ImportSchema, sourceFile: File, listener: (progress: Int, max: Int) -> Unit, dataManager: DataManager): ImportResult
}

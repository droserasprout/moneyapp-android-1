package com.cactusteam.money.data.io

import android.content.Context

/**
 * @author vpotapenko
 */
object ExporterFactory {

    val XLS_TYPE = 0
    val CSV_TYPE = 1

    fun create(context: Context, type: Int): IExporter {
        if (type == XLS_TYPE) {
            return XlsExporter(context)
        } else if (type == CSV_TYPE) {
            return CsvExporter(context)
        } else {
            throw RuntimeException("Not implemented yet")
        }
    }
}

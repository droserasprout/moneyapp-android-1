package com.cactusteam.money.sync

import java.text.SimpleDateFormat
import java.util.Date
import java.util.LinkedList

/**
 * @author vpotapenko
 */
class SyncLogger(previous: List<String>) : ILogger {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    private val lines = LinkedList<String>()

    init {
        lines.addAll(previous)
    }

    fun getLines(): List<String> {
        return lines
    }

    override fun print(message: String) {
        lines.add(String.format("%s: %s", dateFormat.format(Date()), message))

        while (lines.size > MAX_LINES) lines.removeAt(0)
    }

    companion object {

        private val MAX_LINES = 200
    }
}

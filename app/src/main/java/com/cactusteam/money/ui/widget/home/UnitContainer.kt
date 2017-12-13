package com.cactusteam.money.ui.widget.home

import java.util.*

/**
 * @author vpotapenko
 */
class UnitContainer<T : IBaseUnit> {

    private val allUnits: MutableList<T> = mutableListOf()

    fun add(unit: T) {
        allUnits.add(unit)
    }

    fun prepareUnitsByOrder(mainBlockOrder: String): MutableList<T> {
        val units = ArrayList(allUnits)

        val preparedUnits = ArrayList<T>()
        try {
            val parts = mainBlockOrder.split(",".toRegex()).dropLastWhile(String::isEmpty).toTypedArray()
            parts.mapNotNullTo(preparedUnits) { removeUnit(units, it) }
            if (!units.isEmpty()) preparedUnits.addAll(units)
        } catch (e: Exception) {
            e.printStackTrace()
            preparedUnits.addAll(units)
        }

        return preparedUnits
    }

    private fun removeUnit(units: MutableList<T>, part: String): T? {
        val iterator = units.iterator()
        while (iterator.hasNext()) {
            val unit = iterator.next()
            if (part == unit.shortName) {
                iterator.remove()
                return unit
            }
        }
        return null
    }
}

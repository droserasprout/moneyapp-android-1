package com.cactusteam.money.ui.widget.calculator

import org.json.JSONArray
import org.json.JSONObject
import java.math.BigDecimal
import java.util.*

/**
 * @author vpotapenko
 */
class CalculatorController {

    private val values = LinkedList<ICalcValue>()
    private var currentValue: StringBuilder? = null

    fun initialize(obj: JSONObject) {
        val current = obj.optString("current")
        if (current != null) currentValue = StringBuilder(current)

        values.clear()
        val array = obj.getJSONArray("values")
        (0..array.length() - 1)
                .map { array.optJSONObject(it) }
                .filterNotNull()
                .map {
                    when (it.getString("type")) {
                        CalcDivideValue.TYPE -> CalcDivideValue()
                        CalcDoubleValue.TYPE -> {
                            CalcDoubleValue(BigDecimal.valueOf(it.getDouble("value")))
                        }
                        CalcMinusValue.TYPE -> CalcMinusValue()
                        CalcMultiplyValue.TYPE -> CalcMultiplyValue()
                        CalcPlusValue.TYPE -> CalcPlusValue()
                        else -> null
                    }
                }
                .filterNotNull()
                .forEach {
                    values.add(it)
                }
    }

    fun asJson(): JSONObject {
        val result = JSONObject()
        if (currentValue != null) result.put("current", currentValue.toString())

        val array = JSONArray()
        for (v in values) {
            array.put(v.asJson)
        }
        result.put("values", array)
        return result
    }

    fun setCurrent(value: Double) {
        values.clear()
        currentValue = StringBuilder()
        if (value == 0.0) {
            currentValue!!.append('0')
        } else {
            currentValue!!.append(BigDecimal.valueOf(value).toString())
        }
    }

    fun handleNumber(number: Int) {
        if (currentValue == null) {
            currentValue = StringBuilder()
        }
        if (currentValue!!.length == 1 && currentValue!![0] == '0') {
            currentValue!!.deleteCharAt(0)
            currentValue!!.append(number)
        } else {
            currentValue!!.append(number)
        }
    }

    fun handleDot() {
        if (currentValue == null) {
            currentValue = StringBuilder()
            currentValue!!.append('0')
        }
        if (!currentValue!!.toString().contains(".")) currentValue!!.append('.')
    }

    fun handleBackspace() {
        if (currentValue == null) {
            if (!values.isEmpty()) revertCalcValue()
        } else {
            backspaceOnCurrentValue()
        }
    }

    private fun backspaceOnCurrentValue() {
        val length = currentValue!!.length
        currentValue!!.delete(length - 1, length)

        if (currentValue!!.isEmpty()) {
            if (values.isEmpty()) {
                currentValue!!.append('0')
            } else {
                currentValue = null
            }
        }
    }

    private fun revertCalcValue() {
        val value = values.removeAt(values.size - 1)
        if (value.isOperation) {
            val doubleCalcValue = values.removeAt(values.size - 1) as CalcDoubleValue
            currentValue = StringBuilder(doubleCalcValue.asString)
        } else {
            val doubleCalcValue = value as CalcDoubleValue
            currentValue = StringBuilder(doubleCalcValue.asString)

            backspaceOnCurrentValue()
        }
    }

    fun handlePlus() {
        handleOperation(CalcPlusValue())
    }

    fun handleMinus() {
        handleOperation(CalcMinusValue())
    }

    fun handleMultiply() {
        handleOperation(CalcMultiplyValue())
    }

    fun handleDivide() {
        handleOperation(CalcDivideValue())
    }

    private fun handleOperation(operation: ICalcValue) {
        if (currentValue == null) {
            if (!values.isEmpty() && values.last.isOperation) {
                values.removeLast()
            }
            values.add(operation)
        } else {
            applyCurrentValue()
            values.add(operation)
        }
    }

    private fun applyCurrentValue() {
        var value = BigDecimal.ZERO
        try {
            value = BigDecimal(currentValue!!.toString())
        } catch (ignored: NumberFormatException) {
        }

        values.add(CalcDoubleValue(value))
        currentValue = null
    }

    val asString: String
        get() {
            val sb = StringBuilder()
            for (calcValue in values) {
                sb.append(calcValue.asString)
            }

            if (currentValue != null) {
                sb.append(currentValue)
            }

            return sb.toString()
        }

    fun handleClear() {
        values.clear()
        currentValue = StringBuilder()
        currentValue!!.append('0')
    }

    fun calculate(): Double {
        if (currentValue != null) {
            applyCurrentValue()
        }
        return calculateValues()
    }

    private fun calculateValues(): Double {
        try {
            var result = BigDecimal.ZERO
            var previous: ICalcValue? = null
            for (calcValue in values) {
                result = calcValue.apply(previous, result)
                previous = calcValue
            }
            if (currentValue != null && previous != null) {
                try {
                    val calcValue = CalcDoubleValue(BigDecimal(currentValue!!.toString()))
                    result = calcValue.apply(previous, result)
                } catch (ignored: NumberFormatException) {
                }

            }

            return result.toDouble()
        } catch (e: ArithmeticException) {
            return Double.NaN
        }
    }

    val preliminaryAmountString: String
        get() = "=" + calculateValues().toString()

    fun hasPreliminaryAmount(): Boolean {
        return values.any { it.isOperation }
    }
}

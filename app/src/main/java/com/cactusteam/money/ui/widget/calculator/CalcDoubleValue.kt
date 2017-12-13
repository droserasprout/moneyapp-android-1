package com.cactusteam.money.ui.widget.calculator

import org.json.JSONObject
import java.math.BigDecimal

/**
 * @author vpotapenko
 */
class CalcDoubleValue(val value: BigDecimal) : ICalcValue {

    override val asJson: JSONObject
        get() {
            val obj = JSONObject()
            obj.put("type", TYPE)
            obj.put("value", value.toDouble())
            return obj
        }

    override val isOperation: Boolean
        get() = false

    override val asString: String
        get() = value.toString()

    override fun apply(previous: ICalcValue?, result: BigDecimal): BigDecimal {
        if (previous == null) {
            return value
        } else if (previous is CalcMinusValue) {
            return result.subtract(value)
        } else if (previous is CalcMultiplyValue) {
            return result.multiply(value)
        } else if (previous is CalcDivideValue) {
            return result.divide(value)
        } else if (previous is CalcPlusValue) {
            return result.add(value)
        } else {
            return result
        }
    }

    companion object {
        val TYPE = "value"
    }
}

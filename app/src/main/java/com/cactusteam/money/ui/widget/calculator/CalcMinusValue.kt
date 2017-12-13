package com.cactusteam.money.ui.widget.calculator

import org.json.JSONObject
import java.math.BigDecimal

/**
 * @author vpotapenko
 */
class CalcMinusValue : ICalcValue {

    override val asJson: JSONObject
        get() {
            val obj = JSONObject()
            obj.put("type", TYPE)
            return obj
        }

    override val isOperation: Boolean
        get() = true

    override val asString: String
        get() = " - "

    override fun apply(previous: ICalcValue?, result: BigDecimal): BigDecimal {
        return result
    }

    companion object {
        val TYPE = "minus"
    }
}

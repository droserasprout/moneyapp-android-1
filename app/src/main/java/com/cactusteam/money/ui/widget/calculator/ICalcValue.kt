package com.cactusteam.money.ui.widget.calculator

import org.json.JSONObject
import java.math.BigDecimal

/**
 * @author vpotapenko
 */
interface ICalcValue {

    val isOperation: Boolean

    val asString: String

    val asJson: JSONObject

    fun apply(previous: ICalcValue?, result: BigDecimal): BigDecimal
}

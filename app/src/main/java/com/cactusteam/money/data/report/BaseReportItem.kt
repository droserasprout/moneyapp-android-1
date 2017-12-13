package com.cactusteam.money.data.report

/**
 * @author vpotapenko
 */

abstract class BaseReportItem(val amount: Double) {

    var percent: Float? = null
    var color: Int? = null
}

package com.cactusteam.money.ui.widget.home

import com.cactusteam.money.R
import com.cactusteam.money.data.dao.Category
import com.cactusteam.money.ui.UiConstants
import com.cactusteam.money.ui.fragment.HomeFragment

/**
 * @author vpotapenko
 */
class IncomeChartHomeUnit(homeFragment: HomeFragment) : BaseCategoriesChartHomeUnit(homeFragment, Category.INCOME) {

    override fun getLayoutRes(): Int {
        return R.layout.fragment_home_income_chart_unit
    }


    override val shortName: String
        get() = UiConstants.INCOME_CHART_BLOCK
}

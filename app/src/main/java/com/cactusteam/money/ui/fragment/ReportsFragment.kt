package com.cactusteam.money.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import com.cactusteam.money.R
import com.cactusteam.money.data.dao.Category
import com.cactusteam.money.ui.UiConstants
import com.cactusteam.money.ui.activity.BalanceReportActivity
import com.cactusteam.money.ui.activity.CategoriesReportActivity
import com.cactusteam.money.ui.activity.CategoryReportActivity
import com.cactusteam.money.ui.activity.TagsReportActivity

/**
 * @author vpotapenko
 */
class ReportsFragment : BaseMainFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_reports, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById(R.id.categories_report).setOnClickListener { showCategoriesReport() }

        view.findViewById(R.id.balance_report).setOnClickListener { showBalanceReport() }

        view.findViewById(R.id.tags_report).setOnClickListener { showTagsReport() }

        view.findViewById(R.id.category_report).setOnClickListener { categoryReportClicked() }
    }

    private fun categoryReportClicked() {
        ChooseCategoryFragment.build(false) { item ->
            showCategoryReport(item.first)
        }.show(fragmentManager, "dialog")
    }

    private fun showCategoryReport(category: Category) {
        CategoryReportActivity.actionStart(this, UiConstants.CATEGORY_REPORT_REQUEST_CODE, category.id!!, category.name, category.type)
    }

    private fun showBalanceReport() {
        BalanceReportActivity.actionStart(activity)
    }

    private fun showCategoriesReport() {
        CategoriesReportActivity.actionStart(this, UiConstants.CATEGORIES_REPORT_REQUEST_CODE)
    }

    private fun showTagsReport() {
        TagsReportActivity.actionStart(this, UiConstants.TAGS_REPORT_REQUEST_CODE)
    }

    override fun dataChanged() {
        // do nothing
    }

    companion object {

        fun build(): ReportsFragment {
            return ReportsFragment()
        }
    }
}

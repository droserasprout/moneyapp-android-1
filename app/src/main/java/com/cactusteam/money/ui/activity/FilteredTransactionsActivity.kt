package com.cactusteam.money.ui.activity

import android.app.Activity
import android.app.Fragment
import android.content.Intent
import android.os.Bundle
import android.text.Html
import android.view.View
import android.widget.TextView
import com.cactusteam.money.R
import com.cactusteam.money.data.filter.FilterFactory
import com.cactusteam.money.data.filter.ITransactionFilter
import com.cactusteam.money.ui.UiConstants
import com.cactusteam.money.ui.fragment.TransactionsFragment
import java.util.*

/**
 * @author vpotapenko
 */
class FilteredTransactionsActivity : BaseActivity("FilteredTransactionsActivity") {

    private var filter: ITransactionFilter? = null
    private var description: String? = null
    private var from: Date? = null
    private var to: Date? = null

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        injectExtras()

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_filtered_transactions)

        initializeToolbar()

        val titleView = findViewById(R.id.filter_title) as TextView
        if (description != null) {
            titleView.text = Html.fromHtml(description)
        } else {
            titleView.visibility = View.GONE
        }

        val fragment = TransactionsFragment.build()
        fragment.setFilter(filter)
        if (from != null) fragment.setFromDate(from!!)
        if (to != null) fragment.setToDate(to!!)

        showTransaction(fragment)
    }

    private fun showTransaction(fragment: TransactionsFragment) {
        val fragmentManager = fragmentManager
        val ft = fragmentManager.beginTransaction()
        ft.replace(R.id.content_frame, fragment, "transactions")
        ft.commit()
    }

    private fun injectExtras() {
        val intent = intent
        val extras = intent.extras
        if (extras != null) {
            if (extras.containsKey(UiConstants.EXTRA_FILTER)) {
                val filterStr = extras.getString(UiConstants.EXTRA_FILTER)
                filter = FilterFactory.deserialize(filterStr)
            }
            if (extras.containsKey(UiConstants.EXTRA_NAME)) {
                description = extras.getString(UiConstants.EXTRA_NAME)
            }
            if (extras.containsKey(UiConstants.EXTRA_START)) {
                from = Date(extras.getLong(UiConstants.EXTRA_START))
            }
            if (extras.containsKey(UiConstants.EXTRA_FINISH)) {
                to = Date(extras.getLong(UiConstants.EXTRA_FINISH))
            }
        }
    }

    companion object {

        fun actionStart(activity: Activity, requestCode: Int, filter: ITransactionFilter, description: String?, from: Date?, to: Date?) {
            val intent = Intent(activity, FilteredTransactionsActivity::class.java)
            intent.putExtra(UiConstants.EXTRA_FILTER, FilterFactory.serialize(filter))
            if (description != null) intent.putExtra(UiConstants.EXTRA_NAME, description)
            if (from != null) intent.putExtra(UiConstants.EXTRA_START, from.time)
            if (to != null) intent.putExtra(UiConstants.EXTRA_FINISH, to.time)

            activity.startActivityForResult(intent, requestCode)
        }

        fun actionStart(fragment: Fragment, requestCode: Int, filter: ITransactionFilter, description: String?, from: Date?, to: Date?) {
            val intent = Intent(fragment.activity, FilteredTransactionsActivity::class.java)
            intent.putExtra(UiConstants.EXTRA_FILTER, FilterFactory.serialize(filter))
            if (description != null) intent.putExtra(UiConstants.EXTRA_NAME, description)
            if (from != null) intent.putExtra(UiConstants.EXTRA_START, from.time)
            if (to != null) intent.putExtra(UiConstants.EXTRA_FINISH, to.time)

            fragment.startActivityForResult(intent, requestCode)
        }
    }
}

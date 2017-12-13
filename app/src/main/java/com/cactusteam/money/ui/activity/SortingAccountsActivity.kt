package com.cactusteam.money.ui.activity

import android.app.Activity
import android.app.Fragment
import android.content.Intent
import android.os.Bundle
import android.support.v4.util.ArrayMap
import android.support.v7.widget.LinearLayoutManager
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import com.cactusteam.money.R
import com.cactusteam.money.app.MoneyApp
import com.cactusteam.money.data.dao.Account
import com.cactusteam.money.ui.widget.AccountOrderAdapter
import com.woxthebox.draglistview.DragListView

/**
 * @author vpotapenko
 */
class SortingAccountsActivity : BaseDataActivity("SortingAccountsActivity") {

    private var listView: DragListView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sorting_accounts)

        initializeToolbar()
        initializeViewProgress()

        val appPreferences = MoneyApp.instance.appPreferences

        val typesSpinner = findViewById(R.id.sorting_types) as Spinner
        val adapter = ArrayAdapter(this,
                R.layout.activity_sorting_accounts_type,
                android.R.id.text1,
                arrayOf(getString(R.string.sorting_type_and_name), getString(R.string.sorting_name), getString(R.string.sorting_custom)))
        adapter.setDropDownViewResource(android.R.layout.simple_list_item_1)
        typesSpinner.adapter = adapter
        typesSpinner.setSelection(appPreferences.accountSortType)
        typesSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                appPreferences.accountSortType = position
                updateListView(position)

                setResult(Activity.RESULT_OK)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // do nothing
            }
        }

        listView = findViewById(R.id.list) as DragListView
        listView!!.setDragListListener(object : DragListView.DragListListenerAdapter() {
            override fun onItemDragEnded(fromPosition: Int, toPosition: Int) {
                updateOrderAccounts()
            }
        })

        listView!!.setLayoutManager(LinearLayoutManager(this))
        listView!!.setCanDragHorizontally(false)

        listView!!.setAdapter(AccountOrderAdapter(this), true)
        loadAccounts()
    }

    private fun updateListView(position: Int) {
        if (position == Account.CUSTOM_SORT) {
            listView!!.visibility = View.VISIBLE
        } else {
            listView!!.visibility = View.GONE
        }
    }

    private fun loadAccounts() {
        showProgress()
        val s = dataManager.accountService
                .getAccounts()
                .subscribe(
                        { r ->
                            hideProgress()
                            accountsLoaded(r)
                        },
                        { e ->
                            hideProgress()
                            showError(e.message)
                        }
                )
        compositeSubscription.add(s)
    }

    private fun accountsLoaded(accounts: List<Account>) {
        val adapter = listView!!.adapter as AccountOrderAdapter
        adapter.itemList = accounts
    }

    private fun updateOrderAccounts() {
        val order = ArrayMap<Long, Int>()
        val adapter = listView!!.adapter as AccountOrderAdapter
        val itemList = adapter.itemList
        for (i in itemList.indices) {
            val account = itemList[i]
            order.put(account.id, i)
        }

        showProgress()
        val s = dataManager.accountService
                .updateAccountsOrder(order)
                .subscribe(
                        {},
                        { e ->
                            hideProgress()
                            showError(e.message)
                        },
                        {
                            hideProgress()
                        }
                )
        compositeSubscription.add(s)
    }

    companion object {

        fun actionStart(activity: Activity, requestCode: Int) {
            val intent = Intent(activity, SortingAccountsActivity::class.java)
            activity.startActivityForResult(intent, requestCode)
        }

        fun actionStart(fragment: Fragment, requestCode: Int) {
            val intent = Intent(fragment.activity, SortingAccountsActivity::class.java)
            fragment.startActivityForResult(intent, requestCode)
        }
    }
}

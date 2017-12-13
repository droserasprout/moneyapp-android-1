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
import com.cactusteam.money.data.dao.Category
import com.cactusteam.money.ui.UiConstants
import com.cactusteam.money.ui.widget.CategoryOrderAdapter
import com.woxthebox.draglistview.DragListView

/**
 * @author vpotapenko
 */
class SortingCategoriesActivity : BaseDataActivity("SortingCategoriesActivity") {

    private var type: Int = 0

    private var listView: DragListView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        injectExtras()

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sorting_categories)

        setTitle(if (type == Category.EXPENSE) R.string.sorting_expense_categories else R.string.sorting_income_categories)

        initializeToolbar()
        initializeViewProgress()

        val appPreferences = MoneyApp.instance.appPreferences

        val typesSpinner = findViewById(R.id.sorting_types) as Spinner
        val adapter = ArrayAdapter(this,
                R.layout.activity_sorting_categories_type,
                android.R.id.text1,
                arrayOf(getString(R.string.sorting_name), getString(R.string.sorting_frequency), getString(R.string.sorting_custom)))
        adapter.setDropDownViewResource(android.R.layout.simple_list_item_1)
        typesSpinner.adapter = adapter
        typesSpinner.setSelection(if (type == Category.EXPENSE) appPreferences.expenseSortType else appPreferences.incomeSortType)
        typesSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (type == Category.EXPENSE) {
                    appPreferences.expenseSortType = position
                } else {
                    appPreferences.incomeSortType = position
                }

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
                updateOrderCategories()
            }
        })

        listView!!.setLayoutManager(LinearLayoutManager(this))
        listView!!.setCanDragHorizontally(false)

        listView!!.setAdapter(CategoryOrderAdapter(this), true)
        loadCategories()
    }

    override fun onDestroy() {
        (listView!!.adapter as CategoryOrderAdapter).destroy()
        super.onDestroy()
    }

    private fun updateListView(position: Int) {
        if (position == Category.CUSTOM_SORT) {
            listView!!.visibility = View.VISIBLE
        } else {
            listView!!.visibility = View.GONE
        }
    }

    private fun loadCategories() {
        showProgress()
        val s = dataManager.categoryService
                .getCategories(type)
                .subscribe(
                        { r ->
                            hideProgress()
                            categoriesLoaded(r)
                        },
                        { e ->
                            hideProgress()
                            showError(e.message)
                        }
                )
        compositeSubscription.add(s)
    }

    private fun categoriesLoaded(categories: List<Category>) {
        val adapter = listView!!.adapter as CategoryOrderAdapter
        adapter.itemList = categories
    }

    private fun updateOrderCategories() {
        val order = ArrayMap<Long, Int>()
        val adapter = listView!!.adapter as CategoryOrderAdapter
        val itemList = adapter.itemList
        for (i in itemList.indices) {
            val category = itemList[i]
            order.put(category.id, i)
        }

        showProgress()
        val s = dataManager.categoryService
                .updateCategoriesOrder(order)
                .subscribe(
                        {},
                        { e ->
                            hideProgress()
                            showError(e.message)
                        },
                        { hideProgress()}
                )
        compositeSubscription.add(s)
    }

    private fun injectExtras() {
        val intent = intent
        val extras = intent.extras
        if (extras != null) {
            if (extras.containsKey(UiConstants.EXTRA_TYPE)) {
                type = extras.getInt(UiConstants.EXTRA_TYPE)
            }
        }
    }

    companion object {

        fun actionStart(activity: Activity, type: Int, requestCode: Int) {
            val intent = Intent(activity, SortingCategoriesActivity::class.java)
            intent.putExtra(UiConstants.EXTRA_TYPE, type)
            activity.startActivityForResult(intent, requestCode)
        }

        fun actionStart(fragment: Fragment, type: Int, requestCode: Int) {
            val intent = Intent(fragment.activity, SortingCategoriesActivity::class.java)
            intent.putExtra(UiConstants.EXTRA_TYPE, type)
            fragment.startActivityForResult(intent, requestCode)
        }
    }
}

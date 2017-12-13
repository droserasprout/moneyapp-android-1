package com.cactusteam.money.ui.activity

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.support.v4.util.ArrayMap
import android.support.v4.view.MenuItemCompat
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.SearchView
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import com.cactusteam.money.R
import com.cactusteam.money.data.dao.Category
import com.cactusteam.money.data.dao.Subcategory
import com.cactusteam.money.ui.ListItem
import com.cactusteam.money.ui.UiConstants
import com.cactusteam.money.ui.UiObjectRef


class SelectCategoryActivity : BaseDataActivity("SelectCategoryActivity") {

    private var type: Int = 0

    private var listView: RecyclerView? = null

    private val icons = ArrayMap<String, UiObjectRef>()
    private var mockBitmap: Bitmap? = null

    private var showAll: Boolean = false

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == UiConstants.EDIT_CATEGORY_REQUEST_CODE) {
            if (resultCode == RESULT_OK && data != null) {
                val d = Intent()
                d.putExtra(UiConstants.EXTRA_CATEGORY, data.getLongExtra(UiConstants.EXTRA_ID, -1))
                d.putExtra(UiConstants.EXTRA_TYPE, type)
                d.putExtra(UiConstants.EXTRA_CHANGES, true)
                setResult(Activity.RESULT_OK, d)
                finish()
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val menuInflater = menuInflater
        menuInflater.inflate(R.menu.activity_select_category, menu)

        val searchItem = menu.findItem(R.id.action_search)
        val searchView = MenuItemCompat.getActionView(searchItem) as SearchView
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                searchTextChanged(newText)
                return false
            }

        })
        searchView.setOnCloseListener {
            searchClosed()
            false
        }

        return super.onCreateOptionsMenu(menu)
    }

    private fun searchTextChanged(newText: String?) {
        val adapter = listView!!.adapter as ListAdapter
        adapter.applyFilter(newText)
        if (!showAll) {
            showAllClicked()
        } else {
            adapter.notifyDataSetChanged()
        }
    }

    private fun searchClosed() {
        searchTextChanged(null)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val itemId = item.itemId
        if (itemId == R.id.new_category) {
            newCategoryClicked()
            return true
        } else {
            return super.onOptionsItemSelected(item)
        }
    }

    private fun newCategoryClicked() {
        EditCategoryActivity.actionStart(this, UiConstants.EDIT_CATEGORY_REQUEST_CODE, type)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        if (savedInstanceState == null) {
            injectExtras()
        } else {
            restoreState(savedInstanceState)
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_category)

        initializeViewProgress()
        initializeToolbar()

        listView = findViewById(R.id.list) as RecyclerView
        listView!!.layoutManager = LinearLayoutManager(this)
        listView!!.adapter = ListAdapter()

        loadData()
    }

    private fun loadData() {
        showProgress()
        val s = if (showAll) {
            dataManager.categoryService
                    .getCategories(type)
                    .subscribe(
                            { r ->
                                hideProgress()
                                showCategories(r)
                            },
                            { e ->
                                hideProgress()
                                showError(e.message)
                            }
                    )
        } else {
            dataManager.categoryService
                    .getFrequentlyCategories(type)
                    .subscribe(
                            { r ->
                                hideProgress()
                                showCategories(r)
                            },
                            { e ->
                                hideProgress()
                                showError(e.message)
                            }
                    )
        }
        compositeSubscription.add(s)
    }

    private fun showCategories(list: List<Category>?) {
        if (list == null && !showAll) {
            showAll = true
            loadData()
        } else {
            val adapter = listView!!.adapter as ListAdapter
            adapter.allItems.clear()
            list?.forEach { c ->
                adapter.allItems.add(ListItem(CATEGORY_ITEM, c))
                c.subcategories
                        .filterNot { it.deleted }
                        .forEach { adapter.allItems.add(ListItem(SUBCATEGORY_ITEM, it)) }
            }
            if (!showAll) adapter.allItems.add(ListItem(SHOW_ALL_ITEM, null))
            adapter.applyFilter()
            adapter.notifyDataSetChanged()
        }
    }

    override fun onDestroy() {
        for ((key, value) in icons) {
            if (value.ref != null) value.getRefAs(Bitmap::class.java).recycle()
        }
        if (mockBitmap != null && !mockBitmap!!.isRecycled) {
            mockBitmap!!.recycle()
        }
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        outState?.putInt(UiConstants.EXTRA_TYPE, type)

        super.onSaveInstanceState(outState)
    }

    private fun restoreState(savedInstanceState: Bundle) {
        type = savedInstanceState.getInt(UiConstants.EXTRA_TYPE)
    }

    private fun injectExtras() {
        val extras = intent.extras
        if (extras != null) {
            if (extras.containsKey(UiConstants.EXTRA_TYPE)) {
                type = extras.getInt(UiConstants.EXTRA_TYPE)
            }
        }
    }

    private fun subcategoryClicked(s: Subcategory) {
        val c = s.category
        val data = Intent()
        data.putExtra(UiConstants.EXTRA_CATEGORY, c.id)
        data.putExtra(UiConstants.EXTRA_SUBCATEGORY, s.id)
        data.putExtra(UiConstants.EXTRA_TYPE, c.type)
        setResult(Activity.RESULT_OK, data)
        finish()
    }

    private fun showAllClicked() {
        showAll = true
        loadData()
    }

    private fun categoryClicked(c: Category) {
        val data = Intent()
        data.putExtra(UiConstants.EXTRA_CATEGORY, c.id)
        data.putExtra(UiConstants.EXTRA_TYPE, c.type)
        setResult(Activity.RESULT_OK, data)
        finish()
    }

    private fun requestCategoryIcon(iconKey: String) {
        val icon = UiObjectRef()
        icons.put(iconKey, icon)

        val s = dataManager.systemService
                .getIconImage(iconKey)
                .subscribe(
                        { r ->
                            icon.ref = r
                            (listView!!.adapter as ListAdapter).notifyDataSetChanged()
                        },
                        { e -> showError(e.message) }
                )
        compositeSubscription.add(s)
    }

    private fun getMockBitmap(): Bitmap? {
        if (mockBitmap == null) {
            mockBitmap = BitmapFactory.decodeResource(resources, R.drawable.ic_mock_category)
        }
        return mockBitmap
    }

    private fun updateIconView(view: View, category: Category) {
        val iconView = view.findViewById(R.id.category_icon) as ImageView
        val icon = category.icon
        if (icon != null) {
            val categoryIcon = icons[icon]
            if (categoryIcon == null) {
                requestCategoryIcon(icon)
            } else {
                val drawable = BitmapDrawable(resources, if (categoryIcon.ref != null) categoryIcon.getRefAs(Bitmap::class.java) else getMockBitmap())
                drawable.setColorFilter(Color.GRAY, PorterDuff.Mode.SRC_ATOP)
                iconView.setImageDrawable(drawable)
            }
        } else {
            val drawable = BitmapDrawable(resources, getMockBitmap())
            drawable.setColorFilter(Color.GRAY, PorterDuff.Mode.SRC_ATOP)
            iconView.setImageDrawable(drawable)
        }
    }

    inner class CategoryItemHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bindCategory(c: Category) {
            (itemView.findViewById(R.id.name) as TextView).text = c.name
            updateIconView(itemView, c)

            itemView.findViewById(R.id.list_item).setOnClickListener {
                categoryClicked(c)
            }
        }

        fun bindSubcategory(s: Subcategory) {
            (itemView.findViewById(R.id.name) as TextView).text = s.name
            itemView.findViewById(R.id.list_item).setOnClickListener {
                subcategoryClicked(s)
            }
        }

        fun bindShowAll() {
            itemView.findViewById(R.id.show_all_btn)?.setOnClickListener { showAllClicked() }
        }
    }

    private inner class ListAdapter : RecyclerView.Adapter<CategoryItemHolder>() {

        private val items = mutableListOf<ListItem>()
        private var currentFilter: String? = null

        val allItems = mutableListOf<ListItem>()

        override fun onBindViewHolder(holder: CategoryItemHolder?, position: Int) {
            when (getItemViewType(position)) {
                CATEGORY_ITEM -> holder?.bindCategory(items[position].obj as Category)
                SUBCATEGORY_ITEM -> holder?.bindSubcategory(items[position].obj as Subcategory)
                SHOW_ALL_ITEM -> holder?.bindShowAll()
            }
        }

        override fun getItemViewType(position: Int): Int {
            return items[position].type
        }

        override fun getItemCount(): Int {
            return items.size
        }

        override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): CategoryItemHolder {
            var layoutId = 0
            when (viewType) {
                CATEGORY_ITEM -> layoutId = R.layout.activity_select_category_item
                SUBCATEGORY_ITEM -> layoutId = R.layout.activity_select_category_subcategory_item
                SHOW_ALL_ITEM -> layoutId = R.layout.activity_select_category_show_all
            }
            val v = LayoutInflater.from(parent!!.context).inflate(layoutId, parent, false)
            return CategoryItemHolder(v)
        }

        fun applyFilter(txt: String?) {
            currentFilter = txt
            applyFilter()
        }

        fun applyFilter() {
            if (currentFilter.isNullOrBlank()) {
                items.clear()
                items.addAll(allItems)
            } else {
                items.clear()
                allItems.filterTo(items) {
                    when (it.type) {
                        CATEGORY_ITEM -> {
                            val c = it.obj as Category
                            c.name.contains(currentFilter as CharSequence, true)
                        }
                        SUBCATEGORY_ITEM -> {
                            val s = it.obj as Subcategory
                            s.name.contains(currentFilter as CharSequence, true)
                        }
                        else -> true
                    }
                }
            }
        }

    }

    companion object {

        val CATEGORY_ITEM = 0
        val SUBCATEGORY_ITEM = 1
        val SHOW_ALL_ITEM = 2

        fun actionStart(activity: Activity, requestCode: Int, type: Int) {
            val intent = Intent(activity, SelectCategoryActivity::class.java)
            intent.putExtra(UiConstants.EXTRA_TYPE, type)
            activity.startActivityForResult(intent, requestCode)
        }
    }
}

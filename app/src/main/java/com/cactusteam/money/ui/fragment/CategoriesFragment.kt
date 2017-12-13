package com.cactusteam.money.ui.fragment

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.support.v4.util.ArrayMap
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.Html
import android.text.format.DateUtils
import android.view.*
import android.widget.*
import com.cactusteam.money.R
import com.cactusteam.money.app.MoneyApp
import com.cactusteam.money.data.dao.Category
import com.cactusteam.money.data.dao.Subcategory
import com.cactusteam.money.data.model.CategoryAmounts
import com.cactusteam.money.ui.HtmlTagHandler
import com.cactusteam.money.ui.UiConstants
import com.cactusteam.money.ui.UiObjectRef
import com.cactusteam.money.ui.UiUtils
import com.cactusteam.money.ui.activity.*
import rx.Observable
import java.util.*

/**
 * @author vpotapenko
 */
class CategoriesFragment : BaseMainFragment() {

    private val tagHandler = HtmlTagHandler()

    private var typeSpinner: Spinner? = null
    private var listView: RecyclerView? = null

    private val icons = ArrayMap<String, UiObjectRef>()

    private var amounts: CategoryAmounts? = null
    private var currencyCode: String? = null

    private var includeDeleted = false
    private var mockBitmap: Bitmap? = null

    override fun onDestroyView() {
        for ((key, value) in icons) {
            if (value.ref != null) {
                val bitmap = value.getRefAs(Bitmap::class.java)
                if (!bitmap.isRecycled) bitmap.recycle()
            }
        }
        if (mockBitmap != null && !mockBitmap!!.isRecycled) {
            mockBitmap!!.recycle()
        }
        super.onDestroyView()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == UiConstants.EDIT_CATEGORY_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                loadData()
            }
        } else if (requestCode == UiConstants.CATEGORY_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                loadData()
            }
        } else if (requestCode == UiConstants.CATEGORIES_REPORT_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                loadData()
            }
        } else if (requestCode == UiConstants.SUBCATEGORY_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                loadData()
            }
        } else if (requestCode == UiConstants.SORTING_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                loadData()
            }
        } else if (requestCode == UiConstants.NEW_CATEGORY_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                loadData()

                val id = data!!.getLongExtra(UiConstants.EXTRA_ID, 0)
                val name = data.getStringExtra(UiConstants.EXTRA_NAME)
                val type = data.getIntExtra(UiConstants.EXTRA_TYPE, 0)

                showCategoryActivity(id, name, type)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.fragment_categories, menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        menu.findItem(R.id.show_deleted).isVisible = !includeDeleted
        menu.findItem(R.id.hide_deleted).isVisible = includeDeleted

        super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val itemId = item.itemId
        if (itemId == R.id.report) {
            showReportActivity()
            return true
        } else if (itemId == R.id.show_deleted) {
            showDeleted()
            return true
        } else if (itemId == R.id.hide_deleted) {
            hideDeleted()
            return true
        } else if (itemId == R.id.create) {
            showNewCategoryActivity()
            return true
        } else if (itemId == R.id.show_sorting) {
            showSortingActivity()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showSortingActivity() {
        val pos = typeSpinner!!.selectedItemPosition
        SortingCategoriesActivity.actionStart(this, if (pos == 0) Category.EXPENSE else Category.INCOME,
                UiConstants.SORTING_REQUEST_CODE)
    }

    private fun hideDeleted() {
        Toast.makeText(activity, R.string.deleted_categories_was_hidden, Toast.LENGTH_SHORT).show()
        includeDeleted = false
        loadData()

        val supportActionBar = (activity as BaseActivity).supportActionBar
        supportActionBar?.invalidateOptionsMenu()
    }

    private fun showDeleted() {
        Toast.makeText(activity, R.string.deleted_categories_was_shown, Toast.LENGTH_SHORT).show()
        includeDeleted = true
        loadData()

        val supportActionBar = (activity as BaseActivity).supportActionBar
        supportActionBar?.invalidateOptionsMenu()
    }

    private fun showReportActivity() {
        CategoriesReportActivity.actionStart(this, UiConstants.CATEGORIES_REPORT_REQUEST_CODE)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_categories, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)

        currencyCode = MoneyApp.instance.appPreferences.mainCurrencyCode

        initializeProgress(view.findViewById(R.id.progress_bar), view.findViewById(R.id.content))

        typeSpinner = view.findViewById(R.id.categories_type) as Spinner
        val adapter = ArrayAdapter(activity,
                R.layout.fragment_categories_item_type,
                android.R.id.text1,
                arrayOf(getString(R.string.expense_label), getString(R.string.income_label)))
        adapter.setDropDownViewResource(android.R.layout.simple_list_item_1)
        typeSpinner!!.adapter = adapter
        typeSpinner!!.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                loadData()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // do nothing
            }
        }

        listView = view.findViewById(R.id.list) as RecyclerView
        listView!!.layoutManager = LinearLayoutManager(activity)
        listView!!.adapter = ListAdapter()

        loadData()
    }

    private fun loadData() {
        val type = if (typeSpinner!!.selectedItemPosition == 0) Category.EXPENSE else Category.INCOME

        showProgress()
        val o1 = dataManager.categoryService.getCategories(type, includeDeleted)
        val o2 = dataManager.categoryService.getCategoryAmounts()
        val s = Observable.zip(o1, o2, { i1, i2 -> Pair(i1, i2) })
                .subscribe(
                        { r ->
                            hideProgress()
                            dataLoaded(r.first, r.second)
                        },
                        { e ->
                            hideProgress()
                            showError(e.message)
                        }
                )
        compositeSubscription.add(s)
    }

    private fun dataLoaded(categories: List<Category>, categoryAmounts: CategoryAmounts) {
        amounts = categoryAmounts

        val active = ArrayList<Category>()
        val inactive = ArrayList<Category>()

        for (category in categories) {
            if (amounts!!.categoryAmounts[category.id] != null) {
                active.add(category)
            } else {
                inactive.add(category)
            }
        }

        val adapter = listView!!.adapter as ListAdapter
        adapter.categories.clear()

        if (!active.isEmpty()) {
            val current = MoneyApp.instance.period.current
            val currentStr = DateUtils.formatDateRange(activity, current.first.time, current.second.time, DateUtils.FORMAT_SHOW_DATE)
            val title = getString(R.string.active_items, currentStr)
            adapter.categories.add(CategoryViewInfo.createSubheader(title))
        }
        addCategories(active, adapter)

        if (!inactive.isEmpty()) {
            adapter.categories.add(CategoryViewInfo.createSubheader(getString(R.string.inactive)))
        }
        addCategories(inactive, adapter)
        adapter.categories.add(CategoryViewInfo.createNewButton())
        adapter.notifyDataSetChanged()
    }

    private fun addCategories(categories: List<Category>, adapter: ListAdapter) {
        for (category in categories) {
            adapter.categories.add(CategoryViewInfo.createCategory(category))
            category.subcategories
                    .filterNot { it.deleted }
                    .forEach { adapter.categories.add(CategoryViewInfo.createSubcategory(it)) }
        }
    }

    private fun requestCategoryIcon(iconKey: String) {
        val icon = UiObjectRef()
        icons.put(iconKey, icon)

        val s = dataManager.systemService.getIconImage(iconKey)
                .subscribe(
                        { r ->
                            icon.ref = r
                            listView?.adapter?.notifyDataSetChanged()
                        },
                        { e ->
                            showError(e.message)
                        }
                )
        compositeSubscription.add(s)
    }

    private fun showCategoryActivity(category: Category) {
        showCategoryActivity(category.id!!, category.name, category.type)
    }

    private fun showCategoryActivity(id: Long, name: String, type: Int) {
        CategoryActivity.actionStart(this, UiConstants.CATEGORY_REQUEST_CODE, id, name, type)
    }

    private fun showNewCategoryActivity() {
        val type = typeSpinner!!.selectedItemPosition
        EditCategoryActivity.actionStart(this, UiConstants.NEW_CATEGORY_REQUEST_CODE, if (type == 1) Category.INCOME else Category.EXPENSE)
    }

    private fun showSubcategoryActivity(subcategory: Subcategory) {
        SubcategoryActivity.actionStart(this, UiConstants.SUBCATEGORY_REQUEST_CODE,
                subcategory.categoryId,
                subcategory.id!!,
                subcategory.name,
                subcategory.category.type)
    }

    override fun dataChanged() {
        loadData()
    }

    // TODO must be replaced by ListItem
    private class CategoryViewInfo {

        var type: Int = 0

        var title: String? = null
        var category: Category? = null
        var subcategory: Subcategory? = null

        companion object {

            fun createCategory(category: Category): CategoryViewInfo {
                val info = CategoryViewInfo()
                info.type = REGULAR
                info.category = category
                return info
            }

            fun createSubcategory(subcategory: Subcategory): CategoryViewInfo {
                val info = CategoryViewInfo()
                info.type = SUBCATEGORY
                info.subcategory = subcategory
                return info
            }

            fun createNewButton(): CategoryViewInfo {
                val info = CategoryViewInfo()
                info.type = NEW_CATEGORY
                return info
            }

            fun createSubheader(title: String): CategoryViewInfo {
                val info = CategoryViewInfo()
                info.type = SUBHEADER
                info.title = title
                return info
            }
        }
    }

    private inner class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        @Suppress("DEPRECATION")
        fun bindItem(info: CategoryViewInfo) {
            val category = info.category
            val nameView = itemView.findViewById(R.id.name) as TextView
            if (category!!.deleted) {
                val s = String.format(UiConstants.DELETED_PATTERN, category.name)
                nameView.text = Html.fromHtml(s, null, tagHandler)
            } else {
                nameView.text = category.name
            }

            itemView.findViewById(R.id.list_item).setOnClickListener { showCategoryActivity(category) }

            val iconView = itemView.findViewById(R.id.category_icon) as ImageView
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

            val balanceView = itemView.findViewById(R.id.balance) as TextView
            val balanceProgress = itemView.findViewById(R.id.balance_progress)
            if (amounts == null) {
                balanceProgress.visibility = View.VISIBLE
                balanceView.visibility = View.GONE
            } else {
                balanceProgress.visibility = View.GONE
                balanceView.visibility = View.VISIBLE

                val amount = amounts!!.categoryAmounts[category.id]
                if (amount != null) {
                    val amountStr = UiUtils.formatCurrency(amount, currencyCode)
                    balanceView.text = amountStr
                } else {
                    balanceView.text = "-"
                }
            }
        }

        fun bindSubcategory(viewInfo: CategoryViewInfo) {
            val subcategory = viewInfo.subcategory

            val nameView = itemView.findViewById(R.id.name) as TextView
            nameView.text = subcategory!!.name

            itemView.findViewById(R.id.list_item).setOnClickListener { showSubcategoryActivity(subcategory) }

            val balanceView = itemView.findViewById(R.id.balance) as TextView
            val balanceProgress = itemView.findViewById(R.id.balance_progress)
            if (amounts == null) {
                balanceProgress.visibility = View.VISIBLE
                balanceView.visibility = View.GONE
            } else {
                balanceProgress.visibility = View.GONE
                balanceView.visibility = View.VISIBLE

                val amount = amounts!!.subcategoryAmounts[subcategory.id]
                if (amount != null) {
                    val amountStr = UiUtils.formatCurrency(amount, currencyCode)
                    balanceView.text = amountStr
                } else {
                    balanceView.text = "-"
                }
            }
        }

        fun bindNewCategory() {
            itemView.findViewById(R.id.create_category_btn).setOnClickListener { showNewCategoryActivity() }
        }

        fun bindSubheader(categoryViewInfo: CategoryViewInfo) {
            (itemView.findViewById(R.id.name) as TextView).text = categoryViewInfo.title
        }
    }

    private fun getMockBitmap(): Bitmap? {
        if (mockBitmap == null) {
            mockBitmap = BitmapFactory.decodeResource(resources, R.drawable.ic_mock_category)
        }
        return mockBitmap
    }

    private inner class ListAdapter : RecyclerView.Adapter<CategoryViewHolder>() {

        val categories = ArrayList<CategoryViewInfo>()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
            var layoutId = 0
            when (viewType) {
                REGULAR -> layoutId = R.layout.fragment_categories_item
                NEW_CATEGORY -> layoutId = R.layout.fragment_categories_new
                SUBHEADER -> layoutId = R.layout.fragment_categories_subheader
                SUBCATEGORY -> layoutId = R.layout.fragment_categories_subcategory
            }
            val v = LayoutInflater.from(activity).inflate(layoutId, parent, false)
            return CategoryViewHolder(v)
        }

        override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
            val type = getItemViewType(position)
            if (type == REGULAR) {
                holder.bindItem(categories[position])
            } else if (type == NEW_CATEGORY) {
                holder.bindNewCategory()
            } else if (type == SUBHEADER) {
                holder.bindSubheader(categories[position])
            } else if (type == SUBCATEGORY) {
                holder.bindSubcategory(categories[position])
            }
        }

        override fun getItemViewType(position: Int): Int {
            return categories[position].type
        }

        override fun getItemCount(): Int {
            return categories.size
        }
    }

    companion object {

        private val REGULAR = 0
        private val NEW_CATEGORY = 1
        private val SUBHEADER = 2
        private val SUBCATEGORY = 3

        fun build(): CategoriesFragment {
            return CategoriesFragment()
        }
    }
}

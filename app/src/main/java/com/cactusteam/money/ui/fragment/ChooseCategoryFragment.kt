package com.cactusteam.money.ui.fragment

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.support.v4.util.ArrayMap
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.cactusteam.money.R
import com.cactusteam.money.data.dao.Category
import com.cactusteam.money.data.dao.Subcategory
import com.cactusteam.money.ui.UiObjectRef

/**
 * @author vpotapenko
 */
class ChooseCategoryFragment : BaseDialogFragment() {

    private var typeSpinner: Spinner? = null
    private var listView: RecyclerView? = null

    private val icons = ArrayMap<String, UiObjectRef>()
    private var mockBitmap: Bitmap? = null

    private var listener: ((item: Pair<Category, Subcategory?>) -> Unit)? = null
    private var showSubcategories: Boolean = false

    override fun onDestroyView() {
        for ((key, value) in icons) {
            if (value.ref != null) value.getRefAs(Bitmap::class.java).recycle()
        }
        if (mockBitmap != null && !mockBitmap!!.isRecycled) {
            mockBitmap!!.recycle()
        }
        super.onDestroyView()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_choose_category, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dialog.setTitle(R.string.choose_category)

        initializeProgress(view.findViewById(R.id.progress_bar), view.findViewById(R.id.content))

        typeSpinner = view.findViewById(R.id.categories_type) as Spinner
        val adapter = ArrayAdapter(activity,
                R.layout.fragment_choose_category_item_type,
                android.R.id.text1,
                arrayOf(getString(R.string.expense_label), getString(R.string.income_label)))
        adapter.setDropDownViewResource(android.R.layout.simple_list_item_1)
        typeSpinner!!.adapter = adapter
        typeSpinner!!.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                loadCategories()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // do nothing
            }
        }

        listView = view.findViewById(R.id.list) as RecyclerView
        listView!!.layoutManager = LinearLayoutManager(activity)
        listView!!.adapter = ListAdapter()

        loadCategories()
    }

    private fun loadCategories() {
        val type = if (typeSpinner!!.selectedItemPosition == 0) Category.EXPENSE else Category.INCOME

        showProgressAsInvisible()
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
        val adapter = listView!!.adapter as ListAdapter
        adapter.categories.clear()
        for (category in categories) {
            adapter.categories.add(Pair(category, null))
            if (showSubcategories) {
                for (sub in category.subcategories) {
                    adapter.categories.add(Pair(category, sub))
                }
            }
        }
        adapter.notifyDataSetChanged()
    }

    private fun itemChosen(item: Pair<Category, Subcategory?>) {
        if (listener != null) listener!!(item)
        dismiss()
    }

    private fun getMockBitmap(): Bitmap? {
        if (mockBitmap == null) {
            mockBitmap = BitmapFactory.decodeResource(resources, R.drawable.ic_mock_category)
        }
        return mockBitmap
    }

    private fun requestCategoryIcon(iconKey: String) {
        val icon = UiObjectRef()
        icons.put(iconKey, icon)
        val s = dataManager.systemService
                .getIconImage(iconKey)
                .subscribe(
                        { r ->
                            icon.ref = r
                            listView!!.adapter.notifyDataSetChanged()
                        },
                        { e ->
                            showError(e.message)
                        }
                )
        compositeSubscription.add(s)
    }

    private inner class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bindItem(pair: Pair<Category, Subcategory?>) {
            val nameView = itemView.findViewById(R.id.name) as TextView
            val subcategoryNameView = itemView.findViewById(R.id.subcategory_name) as TextView
            val iconView = itemView.findViewById(R.id.category_icon) as ImageView

            val category = pair.first
            val subcategory = pair.second

            if (subcategory != null) {
                nameView.visibility = View.GONE
                iconView.visibility = View.INVISIBLE

                subcategoryNameView.visibility = View.VISIBLE
                subcategoryNameView.text = subcategory.name
            } else {
                subcategoryNameView.visibility = View.GONE

                nameView.visibility = View.VISIBLE
                iconView.visibility = View.VISIBLE

                nameView.text = category.name
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

            itemView.findViewById(R.id.list_item).setOnClickListener { itemChosen(pair) }
        }
    }

    private inner class ListAdapter : RecyclerView.Adapter<CategoryViewHolder>() {

        val categories: MutableList<Pair<Category, Subcategory?>> = mutableListOf()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
            val layoutInflater = LayoutInflater.from(activity)
            val v = layoutInflater.inflate(R.layout.fragment_choose_category_item, parent, false)
            return CategoryViewHolder(v)
        }

        override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
            holder.bindItem(categories[position])
        }

        override fun getItemCount(): Int {
            return categories.size
        }
    }

    companion object {

        fun build(showSubcategories: Boolean, listener: ((item: Pair<Category, Subcategory?>) -> Unit)?): ChooseCategoryFragment {
            val fragment = ChooseCategoryFragment()
            fragment.listener = listener
            fragment.showSubcategories = showSubcategories
            return fragment
        }
    }
}

package com.cactusteam.money.ui.fragment

import android.graphics.Bitmap
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
import com.cactusteam.money.data.dao.Tag
import com.cactusteam.money.ui.ListItem
import java.util.*

/**
 * @author vpotapenko
 */
class SelectPlanDependenciesFragment : BaseDialogFragment() {

    var planDependenciesListener: PlanDependenciesListener? = null

    private var listView: RecyclerView? = null
    private var mainSelectionView: ImageView? = null

    private val icons = ArrayMap<String, Icon>()

    private var mainSelection: Boolean = false

    override fun onDestroyView() {
        for ((key, value) in icons) {
            value.bitmap?.recycle()
        }
        super.onDestroyView()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_select_plan_dependencies, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dialog.setTitle(R.string.add_dependency)

        initializeProgress(view.findViewById(R.id.progress_bar), view.findViewById(R.id.content))

        val typeSpinner = view.findViewById(R.id.type) as Spinner
        val adapter = ArrayAdapter(activity,
                R.layout.fragment_select_plan_dependencies_item_type,
                android.R.id.text1,
                arrayOf(getString(R.string.category_label), getString(R.string.tag_label)))
        adapter.setDropDownViewResource(android.R.layout.simple_list_item_1)
        typeSpinner.adapter = adapter
        typeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                when (position) {
                    0 -> loadCategories()
                    1 -> loadTags()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // do nothing
            }
        }

        view.findViewById(R.id.cancel_btn).setOnClickListener { dismiss() }
        view.findViewById(R.id.ok_btn).setOnClickListener { okClicked() }

        listView = view.findViewById(R.id.list) as RecyclerView
        listView!!.layoutManager = LinearLayoutManager(activity)
        listView!!.adapter = ListAdapter()

        mainSelectionView = view.findViewById(R.id.selection) as ImageView?
        updateSelectionView()

        view.findViewById(R.id.selection_container).setOnClickListener { mainSelectionClicked() }

        loadCategories()
    }

    private fun updateSelectionView() {
        mainSelectionView?.setImageResource(if (mainSelection) R.drawable.ic_checked_primary_24dp else R.drawable.ic_unchecked_primary_24dp)
    }

    private fun mainSelectionClicked() {
        mainSelection = !mainSelection
        updateSelectionView()

        val adapter = listView!!.adapter as ListAdapter
        adapter.items.forEach { it.selected = mainSelection }
        adapter.notifyDataSetChanged()
    }

    private fun updateMainSelection(selected: Boolean) {
        if (!selected) {
            mainSelection = false
            updateSelectionView()
        }
        listView?.adapter?.notifyDataSetChanged()
    }

    private fun okClicked() {
        val adapter = listView!!.adapter as ListAdapter
        adapter.items
                .filter { it.selected }
                .forEach {
                    when (it.type) {
                        CATEGORY_TYPE -> {
                            planDependenciesListener?.categorySelected(it.obj as Category)
                        }
                        SUBCATEGORY_TYPE -> {
                            planDependenciesListener?.subcategorySelected(it.obj as Subcategory)
                        }
                        TAG_TYPE -> {
                            planDependenciesListener?.tagSelected(it.obj as Tag)
                        }
                    }
                }
        dismiss()
    }

    private fun loadTags() {
        showProgressAsInvisible()
        val s = dataManager.tagService
                .getTags()
                .subscribe(
                        { r ->
                            hideProgress()
                            tagsLoaded(r)
                        },
                        { e ->
                            hideProgress()
                            showError(e.message)
                        }
                )
        compositeSubscription.add(s)
    }

    private fun tagsLoaded(tags: List<Tag>) {
        val adapter = listView!!.adapter as ListAdapter
        adapter.items.clear()

        for (tag in tags) {
            adapter.items.add(ListItem(TAG_TYPE, tag))
        }
        adapter.notifyDataSetChanged()
    }

    private fun loadCategories() {
        showProgressAsInvisible()
        val s = dataManager.categoryService
                .getCategories(Category.EXPENSE)
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
        adapter.items.clear()

        for (category in categories) {
            adapter.items.add(ListItem(CATEGORY_TYPE, category))
            for (subcategory in category.subcategories) {
                adapter.items.add(ListItem(SUBCATEGORY_TYPE, subcategory))
            }
        }
        adapter.notifyDataSetChanged()
    }

    private fun requestCategoryIcon(iconKey: String) {
        val icon = Icon()
        icons.put(iconKey, icon)
        val s = dataManager.systemService
                .getIconImage(iconKey)
                .subscribe(
                        { r ->
                            icon.bitmap = r
                            listView!!.adapter.notifyDataSetChanged()
                        },
                        { e ->
                            showError(e.message)
                        }
                )
        compositeSubscription.add(s)
    }

    interface PlanDependenciesListener {

        fun categorySelected(category: Category)

        fun subcategorySelected(subcategory: Subcategory)

        fun tagSelected(tag: Tag)
    }

    private inner class ListViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bindCategory(item: ListItem) {
            val category = item.obj as Category
            val nameView = itemView.findViewById(R.id.name) as TextView
            nameView.text = category.name
            nameView.visibility = View.VISIBLE
            itemView.findViewById(R.id.subcategory_name).visibility = View.GONE

            val iconView = itemView.findViewById(R.id.icon) as ImageView
            iconView.visibility = View.VISIBLE
            iconView.setImageResource(R.drawable.ic_mock_icon)

            val icon = category.icon
            if (icon != null) {
                val categoryIcon = icons[icon]
                if (categoryIcon == null) {
                    requestCategoryIcon(icon)
                } else {
                    if (categoryIcon.bitmap != null) {
                        val drawable = BitmapDrawable(resources, categoryIcon.bitmap)
                        drawable.setColorFilter(Color.GRAY, PorterDuff.Mode.SRC_ATOP)
                        iconView.setImageDrawable(drawable)
                    }
                }
            }

            val selectionView = itemView.findViewById(R.id.selection) as ImageView
            selectionView.setImageResource(if (item.selected) R.drawable.ic_checked_primary_24dp else R.drawable.ic_unchecked_primary_24dp)

            itemView.findViewById(R.id.list_item).setOnClickListener {
                item.selected = !item.selected
                updateMainSelection(item.selected)
            }
        }

        fun bindSubcategory(item: ListItem) {
            val subcategory = item.obj as Subcategory
            itemView.findViewById(R.id.name).visibility = View.GONE
            itemView.findViewById(R.id.icon).visibility = View.INVISIBLE

            val nameView = itemView.findViewById(R.id.subcategory_name) as TextView
            nameView.text = subcategory.name
            nameView.visibility = View.VISIBLE

            val selectionView = itemView.findViewById(R.id.selection) as ImageView
            selectionView.setImageResource(if (item.selected) R.drawable.ic_checked_primary_24dp else R.drawable.ic_unchecked_primary_24dp)

            itemView.findViewById(R.id.list_item).setOnClickListener {
                item.selected = !item.selected
                updateMainSelection(item.selected)
            }
        }

        fun bindTag(item: ListItem) {
            val tag = item.obj as Tag
            val nameView = itemView.findViewById(R.id.name) as TextView
            nameView.text = tag.name
            nameView.visibility = View.VISIBLE
            itemView.findViewById(R.id.subcategory_name).visibility = View.GONE

            val iconView = itemView.findViewById(R.id.icon) as ImageView
            iconView.visibility = View.VISIBLE
            iconView.setImageResource(R.drawable.ic_tags)

            val selectionView = itemView.findViewById(R.id.selection) as ImageView
            selectionView.setImageResource(if (item.selected) R.drawable.ic_checked_primary_24dp else R.drawable.ic_unchecked_primary_24dp)

            itemView.findViewById(R.id.list_item).setOnClickListener {
                item.selected = !item.selected
                updateMainSelection(item.selected)
            }
        }
    }

    private inner class ListAdapter : RecyclerView.Adapter<ListViewHolder>() {

        val items = ArrayList<ListItem>()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListViewHolder {
            val v = LayoutInflater.from(activity).inflate(R.layout.fragment_select_plan_dependencies_item, parent, false)
            return ListViewHolder(v)
        }

        override fun onBindViewHolder(holder: ListViewHolder, position: Int) {
            when (getItemViewType(position)) {
                CATEGORY_TYPE -> holder.bindCategory(items[position])
                SUBCATEGORY_TYPE -> holder.bindSubcategory(items[position])
                TAG_TYPE -> holder.bindTag(items[position])
            }
        }

        override fun getItemViewType(position: Int): Int {
            return items[position].type
        }

        override fun getItemCount(): Int {
            return items.size
        }
    }

    private class Icon {

        var bitmap: Bitmap? = null
    }

    companion object {

        private val CATEGORY_TYPE = 0
        private val SUBCATEGORY_TYPE = 1
        private val TAG_TYPE = 2

        fun build(planDependenciesListener: PlanDependenciesListener): SelectPlanDependenciesFragment {
            val fragment = SelectPlanDependenciesFragment()
            fragment.planDependenciesListener = planDependenciesListener
            return fragment
        }
    }
}

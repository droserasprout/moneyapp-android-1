package com.cactusteam.money.ui.widget

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.BitmapDrawable
import android.support.v4.util.ArrayMap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.cactusteam.money.R
import com.cactusteam.money.data.dao.Category
import com.cactusteam.money.ui.UiObjectRef
import com.cactusteam.money.ui.activity.SortingCategoriesActivity
import com.woxthebox.draglistview.DragItemAdapter

/**
 * @author vpotapenko
 */
class CategoryOrderAdapter(private val parent: SortingCategoriesActivity) : DragItemAdapter<Category, CategoryOrderAdapter.ViewHolder>() {
    private val grabHandleId = R.id.drag_area

    private val icons = ArrayMap<String, UiObjectRef>()
    private var mockBitmap: Bitmap? = null

    init {
        setHasStableIds(true)
    }

    fun destroy() {
        for ((key, value) in icons) {
            if (value.ref != null) value.getRefAs(Bitmap::class.java).recycle()
        }
        if (mockBitmap != null && !mockBitmap!!.isRecycled) {
            mockBitmap!!.recycle()
        }
    }

    private fun requestCategoryIcon(iconKey: String) {
        val icon = UiObjectRef()
        icons.put(iconKey, icon)

        val s = parent.dataManager.systemService
                .getIconImage(iconKey)
                .subscribe(
                        { r ->
                            icon.ref = r
                            notifyDataSetChanged()
                        },
                        { e ->
                            parent.showError(e.message)
                        }
                )
        parent.compositeSubscription.add(s)
    }

    private fun getMockBitmap(): Bitmap? {
        if (mockBitmap == null) {
            mockBitmap = BitmapFactory.decodeResource(parent.resources, R.drawable.ic_mock_category)
        }
        return mockBitmap
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.activity_sorting_categories_list_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        super.onBindViewHolder(holder, position)
        holder.bind(itemList[position])
    }

    override fun getItemId(position: Int): Long {
        return mItemList[position].id!!
    }

    inner class ViewHolder(itemView: View) : DragItemAdapter.ViewHolder(itemView, grabHandleId, false) {

        internal fun bind(category: Category) {
            updateIcon(category)
            (itemView.findViewById(R.id.name) as TextView).text = category.name
        }

        private fun updateIcon(category: Category) {
            val icon = category.icon
            if (icon != null) {
                val categoryIcon = icons[icon]
                if (categoryIcon == null) {
                    requestCategoryIcon(icon)
                } else {
                    val drawable = BitmapDrawable(parent.resources, if (categoryIcon.ref != null) categoryIcon.getRefAs(Bitmap::class.java) else getMockBitmap())
                    drawable.setColorFilter(Color.GRAY, PorterDuff.Mode.SRC_ATOP)
                    (itemView.findViewById(R.id.icon) as ImageView).setImageDrawable(drawable)
                }
            } else {
                val drawable = BitmapDrawable(parent.resources, getMockBitmap())
                drawable.setColorFilter(Color.GRAY, PorterDuff.Mode.SRC_ATOP)
                (itemView.findViewById(R.id.icon) as ImageView).setImageDrawable(drawable)
            }
        }
    }
}

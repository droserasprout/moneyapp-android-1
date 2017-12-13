package com.cactusteam.money.ui.fragment

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.support.v4.util.ArrayMap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseExpandableListAdapter
import android.widget.ExpandableListView
import android.widget.ImageView
import android.widget.TextView
import com.cactusteam.money.R
import com.cactusteam.money.data.model.Icons
import com.cactusteam.money.ui.UiObjectRef
import java.util.*

/**
 * @author vpotapenko
 */
class ChooseIconFragment : BaseDialogFragment() {

    var iconListener: ((imagePath: String) -> Unit)? = null

    private var listView: ExpandableListView? = null

    private val icons = ArrayMap<String, UiObjectRef>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_choose_icon, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dialog.setTitle(R.string.category_icon)
        listView = view.findViewById(R.id.list) as ExpandableListView
        listView!!.setAdapter(ListAdapter())
        listView!!.setOnChildClickListener { parent, v, groupPosition, childPosition, id ->
            val path = parent.expandableListAdapter.getChild(groupPosition, childPosition) as String
            iconChosen(path)

            true
        }

        initializeProgress(view.findViewById(R.id.progress_bar), listView!!)

        loadIconPaths()
    }

    private fun loadIconPaths() {
        showProgress()
        val s = dataManager.systemService
                .getIconPaths()
                .subscribe(
                        { r ->
                            hideProgress()
                            iconsLoaded(r)
                        },
                        { e ->
                            hideProgress()
                            showError(e.message)
                        }
                )
        compositeSubscription.add(s)
    }

    private fun iconsLoaded(icons: Icons) {
        val adapter = listView!!.expandableListAdapter as ListAdapter
        adapter.groups.clear()
        adapter.groups.addAll(icons.groups)
        adapter.iconPaths = icons.icons
        adapter.notifyDataSetChanged()
    }


    override fun onDestroyView() {
        for ((key, value) in icons) {
            if (value.ref != null) value.getRefAs(Bitmap::class.java).recycle()
        }
        super.onDestroyView()
    }

    private fun iconChosen(iconPath: String) {
        if (iconListener != null) iconListener!!(iconPath)
        dismiss()
    }

    private fun requestCategoryIcon(iconKey: String) {
        val icon = UiObjectRef()
        icons.put(iconKey, icon)

        val s = dataManager.systemService
                .getIconImage(iconKey)
                .subscribe(
                        { r ->
                            icon.ref = r
                            (listView!!.expandableListAdapter as ListAdapter).notifyDataSetChanged()
                        },
                        { e ->
                            showError(e.message)
                        }
                )
        compositeSubscription.add(s)
    }

    private inner class ListAdapter : BaseExpandableListAdapter() {

        val groups = ArrayList<Pair<String, String>>()

        var iconPaths: MutableMap<String, MutableList<String>> = mutableMapOf()

        override fun getGroupCount(): Int {
            return groups.size
        }

        override fun getChildrenCount(groupPosition: Int): Int {
            val group = groups[groupPosition]
            val list = iconPaths[group.first]
            return list?.size ?: 0
        }

        override fun getGroup(groupPosition: Int): Pair<String, String> {
            return groups[groupPosition]
        }

        override fun getChild(groupPosition: Int, childPosition: Int): String {
            val group = groups[groupPosition]
            val list = iconPaths[group.first]
            return if (list == null) "" else list[childPosition]
        }

        override fun getGroupId(groupPosition: Int): Long {
            return groupPosition.toLong()
        }

        override fun getChildId(groupPosition: Int, childPosition: Int): Long {
            return childPosition.toLong()
        }

        override fun hasStableIds(): Boolean {
            return false
        }

        override fun getGroupView(groupPosition: Int, isExpanded: Boolean, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: View.inflate(parent.context, R.layout.fragment_choose_icon_group, null)

            val group = getGroup(groupPosition)
            (view.findViewById(android.R.id.text1) as TextView).text = group.second

            return view
        }

        override fun getChildView(groupPosition: Int, childPosition: Int, isLastChild: Boolean, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: View.inflate(parent.context, R.layout.fragment_choose_icon_item, null)

            val iconPath = getChild(groupPosition, childPosition)

            val icon = icons[iconPath]
            if (icon == null) {
                requestCategoryIcon(iconPath)
                view.findViewById(R.id.icon_progress).visibility = View.VISIBLE
                view.findViewById(R.id.icon).visibility = View.INVISIBLE
            } else {
                view.findViewById(R.id.icon_progress).visibility = View.GONE

                val iconView = view.findViewById(R.id.icon) as ImageView
                iconView.visibility = View.VISIBLE
                if (icon.ref != null) {
                    val drawable = BitmapDrawable(resources, icon.getRefAs(Bitmap::class.java))
                    drawable.setColorFilter(Color.GRAY, PorterDuff.Mode.SRC_ATOP)
                    iconView.setImageDrawable(drawable)
                } else {
                    iconView.setImageResource(R.drawable.ic_mock_icon)
                }
            }
            (view.findViewById(R.id.list_item) as TextView).text = iconPath

            return view
        }

        override fun isChildSelectable(groupPosition: Int, childPosition: Int): Boolean {
            return true
        }
    }

    companion object {

        fun build(): ChooseIconFragment {
            return ChooseIconFragment()
        }
    }
}

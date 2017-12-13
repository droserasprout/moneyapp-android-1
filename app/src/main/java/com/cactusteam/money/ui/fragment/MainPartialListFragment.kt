package com.cactusteam.money.ui.fragment

import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView

import com.cactusteam.money.R
import com.cactusteam.money.ui.MainSection
import com.cactusteam.money.ui.activity.MainActivity

import java.util.ArrayList

/**
 * @author vpotapenko
 */
class MainPartialListFragment : BaseFragment() {

    private val items = ArrayList<MainNavItem>()

    private var listView: RecyclerView? = null

    private var selectedSection: MainSection? = null

    override fun onSaveInstanceState(outState: Bundle) {
        if (selectedSection != null) outState.putString("section", selectedSection!!.name)

        super.onSaveInstanceState(outState)
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)

        if (savedInstanceState != null) {
            val sectionName = savedInstanceState.getString("section")
            val section = MainSection.find(sectionName)
            if (section != null) selectSection(section)
        }
    }

    fun selectSection(section: MainSection) {
        selectedSection = section
        listView!!.adapter.notifyDataSetChanged()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_main_partial_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        listView = view.findViewById(R.id.list) as RecyclerView
        listView!!.layoutManager = LinearLayoutManager(activity)
        val adapter = ListAdapter()

        items.add(MainNavItem(MainSection.HOME, R.drawable.ic_drawer_home))
        items.add(MainNavItem(MainSection.ACCOUNTS, R.drawable.ic_drawer_accounts))
        items.add(MainNavItem(MainSection.CATEGORIES, R.drawable.ic_drawer_categories))
        items.add(MainNavItem(MainSection.TAGS, R.drawable.ic_drawer_tags))
        items.add(MainNavItem(MainSection.TRANSACTIONS, R.drawable.ic_drawer_transactions))
        items.add(MainNavItem(MainSection.BUDGET, R.drawable.ic_drawer_budget))
        items.add(MainNavItem(MainSection.DEBTS, R.drawable.ic_drawer_debts))
        items.add(MainNavItem(MainSection.REPORTS, R.drawable.ic_drawer_reports))
        items.add(MainNavItem(MainSection.SYNC, R.drawable.ic_drawer_sync))
        items.add(MainNavItem(MainSection.SETTINGS, R.drawable.ic_drawer_settings))
        items.add(MainNavItem(MainSection.DONATION, R.drawable.ic_drawer_donation))

        listView!!.adapter = adapter
    }

    override fun onDestroyView() {
        for (item in items) {
            if (item.activeIcon != null && !item.activeIcon!!.bitmap.isRecycled) {
                item.activeIcon!!.bitmap.recycle()
                item.activeIcon = null
            }
            if (item.normalIcon != null && !item.normalIcon!!.bitmap.isRecycled) {
                item.normalIcon!!.bitmap.recycle()
                item.normalIcon = null
            }
        }
        super.onDestroyView()
    }

    private val mainActivity: MainActivity
        get() = activity as MainActivity

    private inner class ListViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        @Suppress("DEPRECATION")
        fun bindItem(item: MainNavItem) {
            item.updateIcon()

            val imageView = itemView.findViewById(R.id.icon) as ImageView
            if (item.section == selectedSection) {
                imageView.setImageDrawable(item.activeIcon)
                itemView.findViewById(R.id.active_marker).setBackgroundResource(R.drawable.bg_partial_item_active)
            } else {
                imageView.setImageDrawable(item.normalIcon)
                itemView.findViewById(R.id.active_marker).setBackgroundDrawable(null)
            }

            itemView.findViewById(R.id.list_item).setOnClickListener { mainActivity.showSection(item.section) }
        }
    }

    private inner class ListAdapter : RecyclerView.Adapter<ListViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListViewHolder {
            val v = LayoutInflater.from(activity).inflate(R.layout.fragment_main_partial_list_item, parent, false)
            return ListViewHolder(v)
        }

        override fun onBindViewHolder(holder: ListViewHolder, position: Int) {
            holder.bindItem(items[position])
        }

        override fun getItemCount(): Int {
            return items.size
        }
    }

    private inner class MainNavItem(val section: MainSection, val resImgId: Int) {

        var normalIcon: BitmapDrawable? = null
        var activeIcon: BitmapDrawable? = null

        @Suppress("DEPRECATION")
        fun updateIcon() {
            if (section == selectedSection && activeIcon == null) {
                activeIcon = BitmapDrawable(resources, BitmapFactory.decodeResource(resources, resImgId))
                activeIcon!!.setColorFilter(resources.getColor(R.color.color_primary), PorterDuff.Mode.SRC_ATOP)
            }
            if (section != selectedSection && normalIcon == null) {
                normalIcon = BitmapDrawable(resources, BitmapFactory.decodeResource(resources, resImgId))
                normalIcon!!.setColorFilter(Color.GRAY, PorterDuff.Mode.SRC_ATOP)
            }
        }
    }
}

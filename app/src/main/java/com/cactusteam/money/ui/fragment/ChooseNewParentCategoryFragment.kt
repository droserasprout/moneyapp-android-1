package com.cactusteam.money.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ListView

import com.cactusteam.money.R
import com.cactusteam.money.data.dao.Category

/**
 * @author vpotapenko
 */
class ChooseNewParentCategoryFragment : BaseDialogFragment() {

    private var categories: List<Category>? = null
    private var listener: ((category: Category) -> Unit)? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_choose_new_parent_category, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dialog.setTitle(R.string.choose_parent_category)

        val adapter = ArrayAdapter<String>(activity, android.R.layout.simple_list_item_1)
        for (category in categories!!) {
            adapter.add(category.name)
        }
        val listView = view.findViewById(R.id.list) as ListView
        listView.adapter = adapter
        listView.onItemClickListener = AdapterView.OnItemClickListener { adapterView, view, position, id ->
            val category = categories!![position]
            listener!!(category)
            dismiss()
        }
    }

    companion object {

        fun build(categories: List<Category>, listener: ((category: Category) -> Unit)?): ChooseNewParentCategoryFragment {
            val fragment = ChooseNewParentCategoryFragment()
            fragment.categories = categories
            fragment.listener = listener
            return fragment
        }
    }
}

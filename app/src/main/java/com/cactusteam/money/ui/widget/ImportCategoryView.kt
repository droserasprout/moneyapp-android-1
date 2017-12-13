package com.cactusteam.money.ui.widget

import android.content.Context
import android.text.TextUtils
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.cactusteam.money.R
import com.cactusteam.money.data.dao.Category
import com.cactusteam.money.data.io.ExistCategoryImportStrategy
import com.cactusteam.money.data.io.ImportCategory
import com.cactusteam.money.data.io.NewCategoryImportStrategy

/**
 * @author vpotapenko
 */
class ImportCategoryView(context: Context, private val importCategory: ImportCategory, private val allCategories: List<Category>) : LinearLayout(context) {

    private var nameEdit: EditText? = null
    private var actionSpinner: Spinner? = null

    init {

        View.inflate(context, R.layout.activity_import_transactions_action, this)

        initializeView()
    }

    fun apply() {
        val action = actionSpinner!!.selectedItem as CategoryAction?
        if (action != null) apply(action)
    }

    private fun apply(action: CategoryAction) {
        if (action.type == CREATE_CATEGORY) {
            val text = nameEdit!!.text
            val newName = if (TextUtils.isEmpty(text)) importCategory.name else text.toString()

            if (importCategory.strategy is NewCategoryImportStrategy) {
                val categoryImportStrategy = importCategory.strategy as NewCategoryImportStrategy
                categoryImportStrategy.name = newName
            } else {
                importCategory.strategy = NewCategoryImportStrategy(newName)
            }
        } else if (action.type == CHOOSE_CATEGORY) {
            if (importCategory.strategy is ExistCategoryImportStrategy) {
                val categoryImportStrategy = importCategory.strategy as ExistCategoryImportStrategy
                categoryImportStrategy.category = action.category!!
            } else {
                importCategory.strategy = ExistCategoryImportStrategy(action.category!!)
            }
        }
    }

    private fun initializeView() {
        nameEdit = findViewById(R.id.name_edit) as EditText

        val actionsAdapter = ActionsAdapter(context)
        actionsAdapter.add(CategoryAction(CREATE_CATEGORY, context.getString(R.string.create_category)))

        for (category in allCategories) {
            val categoryAction = CategoryAction(CHOOSE_CATEGORY, context.getString(R.string.use_category, category.name))
            categoryAction.category = category

            actionsAdapter.add(categoryAction)
        }
        actionSpinner = findViewById(R.id.action_spinner) as Spinner
        actionSpinner!!.adapter = actionsAdapter
        actionSpinner!!.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                onActionSelected(position)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // do nothing
            }
        }

        if (importCategory.strategy is ExistCategoryImportStrategy) {
            val categoryId = importCategory.strategy.predictCategoryId()!!
            for (i in allCategories.indices) {
                val category = allCategories[i]
                if (category.id === categoryId) {
                    actionSpinner!!.setSelection(i + 1)
                    break
                }
            }
        } else {
            actionSpinner!!.setSelection(0)
        }
    }

    private fun onActionSelected(position: Int) {
        val adapter = actionSpinner!!.adapter as ActionsAdapter
        val categoryAction = adapter.getItem(position)

        if (categoryAction!!.type == CREATE_CATEGORY) {
            nameEdit!!.visibility = View.VISIBLE
            nameEdit!!.setText(importCategory.name)
        } else {
            nameEdit!!.visibility = View.GONE
        }
    }

    private inner class ActionsAdapter(context: Context) : ArrayAdapter<CategoryAction>(context, 0) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: View.inflate(parent.context, R.layout.activity_import_transactions_action_item, null)

            bindView(view, position)
            (view.findViewById(R.id.name) as TextView).text = importCategory.name

            return view
        }

        private fun bindView(view: View, position: Int) {
            val categoryAction = getItem(position)
            (view.findViewById(android.R.id.text1) as TextView).text = categoryAction!!.actionDescription
        }

        override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: View.inflate(parent.context, android.R.layout.simple_list_item_1, null)

            bindView(view, position)

            return view
        }
    }

    private class CategoryAction(val type: Int, val actionDescription: String) {

        var category: Category? = null
    }

    companion object {

        private val CREATE_CATEGORY = 0
        private val CHOOSE_CATEGORY = 1
    }
}

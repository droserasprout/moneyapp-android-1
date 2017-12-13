package com.cactusteam.money.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.cactusteam.money.R

/**
 * @author vpotapenko
 */
class EditSubcategoryFragment : BaseDialogFragment() {

    var listener: ((name: String) -> Unit)? = null

    private var categoryId: Long = -1
    private var subcategoryId: Long = -1
    private var subcategoryName: String? = null

    private var nameView: TextView? = null
    private var errorNameText: TextView? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_new_subcategory, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dialog.setTitle(if (subcategoryId >= 0) R.string.edit else R.string.create_subcategory)

        initializeProgress(view.findViewById(R.id.progress_bar), view.findViewById(R.id.content))

        nameView = view.findViewById(R.id.name) as TextView
        errorNameText = view.findViewById(R.id.name_error) as TextView

        view.findViewById(R.id.ok_btn).setOnClickListener { okClicked() }
        view.findViewById(R.id.cancel_btn).setOnClickListener { dismiss() }

        if (subcategoryId >= 0) nameView!!.text = subcategoryName
    }

    private fun okClicked() {
        clearError()

        val name = nameView!!.text
        if (name.isNullOrBlank()) {
            errorNameText!!.setText(R.string.subcategory_name_is_required)
            errorNameText!!.visibility = View.VISIBLE
            return
        }

        showProgress()

        val newName = name.toString()
        if (subcategoryId < 0) {
            val s = dataManager.categoryService
                    .createSubcategory(categoryId, newName)
                    .subscribe(
                            { r ->
                                hideProgress()
                                if (listener != null) listener!!(newName)

                                dismiss()
                            },
                            { e ->
                                hideProgress()
                                showError(e.message)
                            }
                    )
            compositeSubscription.add(s)
        } else {
            val s = dataManager.categoryService
                    .updateSubcategory(subcategoryId, newName)
                    .subscribe(
                            { r ->
                                hideProgress()
                                if (listener != null) listener!!(newName)
                                dismiss()
                            },
                            { e ->
                                hideProgress()
                                showError(e.message)
                            }
                    )
            compositeSubscription.add(s)
        }
    }

    private fun clearError() {
        errorNameText!!.visibility = View.GONE
    }

    companion object {

        fun buildNew(categoryId: Long): EditSubcategoryFragment {
            val fragment = EditSubcategoryFragment()
            fragment.categoryId = categoryId
            fragment.isCancelable = false
            return fragment
        }

        fun buildEdit(subcategoryId: Long, subcategoryName: String?): EditSubcategoryFragment {
            val fragment = EditSubcategoryFragment()
            fragment.subcategoryId = subcategoryId
            fragment.subcategoryName = subcategoryName
            fragment.isCancelable = false
            return fragment
        }
    }
}

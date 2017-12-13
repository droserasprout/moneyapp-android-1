package com.cactusteam.money.ui.fragment

import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.cactusteam.money.R

/**
 * @author vpotapenko
 */
class EditTagFragment : BaseDialogFragment() {

    private var tagName: String? = null

    private var nameView: TextView? = null
    private var errorNameText: TextView? = null

    private var listener: ((name: String) -> Unit)? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_edit_tag, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dialog.setTitle(R.string.edit)

        initializeProgress(view.findViewById(R.id.progress_bar), view.findViewById(R.id.content))

        nameView = view.findViewById(R.id.name) as TextView
        errorNameText = view.findViewById(R.id.name_error) as TextView

        view.findViewById(R.id.ok_btn).setOnClickListener { okClicked() }
        view.findViewById(R.id.cancel_btn).setOnClickListener { dismiss() }

        nameView!!.text = tagName
    }

    private fun okClicked() {
        clearError()

        val name = nameView!!.text
        if (TextUtils.isEmpty(name)) {
            errorNameText!!.setText(R.string.name_must_not_be_empty)
            errorNameText!!.visibility = View.VISIBLE
            return
        } else if (TextUtils.equals(name, tagName)) {
            dismiss()
            return
        }

        showProgress()
        val newName = name.toString()

        val s = dataManager.tagService
                .updateTag(tagName!!, newName)
                .subscribe(
                        {},
                        { e ->
                            hideProgress()
                            showError(e.message)
                        },
                        {
                            hideProgress()
                            listener!!(newName)
                            dismiss()
                        }
                )
        compositeSubscription.add(s)
    }

    private fun clearError() {
        errorNameText!!.visibility = View.GONE
    }

    companion object {

        fun build(tagName: String, listener: ((name: String) -> Unit)?): EditTagFragment {
            val fragment = EditTagFragment()
            fragment.tagName = tagName
            fragment.listener = listener
            return fragment
        }
    }
}

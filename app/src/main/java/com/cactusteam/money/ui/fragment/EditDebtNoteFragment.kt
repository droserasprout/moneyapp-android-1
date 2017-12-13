package com.cactusteam.money.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import com.cactusteam.money.R
import com.cactusteam.money.data.dao.DebtNote

/**
 * @author vpotapenko
 */
class EditDebtNoteFragment : BaseDialogFragment() {

    private var debtId: Long? = null
    private var note: DebtNote? = null
    private var listener: (() -> Unit)? = null

    private var noteView: EditText? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_edit_debt_note, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dialog.setTitle(if (note == null) R.string.new_note else R.string.edit)

        initializeProgress(view.findViewById(R.id.progress_bar), view.findViewById(R.id.content))

        noteView = view.findViewById(R.id.note) as EditText
        if (note != null) noteView!!.setText(note!!.text)

        view.findViewById(R.id.ok_btn).setOnClickListener { okClicked() }
        view.findViewById(R.id.cancel_btn).setOnClickListener { dismiss() }
    }

    private fun okClicked() {
        val text = noteView?.text
        if (!text.isNullOrBlank()) {
            showProgress()
            val s = if (note == null) {
                dataManager.debtService.createDebtNote(debtId!!, text.toString())
            } else {
                dataManager.debtService.updateDebtNote(note!!.id, text.toString())
            }.subscribe(
                    { r ->
                        hideProgress()
                        listener?.invoke()
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

    companion object {

        fun build(debtId: Long, listener: (() -> Unit)?): EditDebtNoteFragment {
            return build(debtId, null, listener)
        }

        fun build(debtId: Long, note: DebtNote?, listener: (() -> Unit)?): EditDebtNoteFragment {
            val fragment = EditDebtNoteFragment()
            fragment.debtId = debtId
            fragment.note = note
            fragment.listener = listener
            return fragment
        }
    }
}
package com.cactusteam.money.ui.widget.home

import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.cactusteam.money.R
import com.cactusteam.money.data.dao.Note
import com.cactusteam.money.ui.MainSection
import com.cactusteam.money.ui.UiConstants
import com.cactusteam.money.ui.activity.BudgetPlanActivity
import com.cactusteam.money.ui.activity.DebtActivity
import com.cactusteam.money.ui.activity.EditTransactionActivity
import com.cactusteam.money.ui.activity.MainActivity
import com.cactusteam.money.ui.fragment.HomeFragment

/**
 * @author vpotapenko
 */
class NotesHomeUnit(homeFragment: HomeFragment) : BaseHomeUnit(homeFragment) {

    private var itemsContainer: LinearLayout? = null
    private var itemsProgress: View? = null

    override fun getLayoutRes(): Int {
        return R.layout.fragment_home_notes_unit
    }

    override fun initializeView() {
        itemsContainer = getView()!!.findViewById(R.id.items_container) as LinearLayout
        itemsProgress = getView()!!.findViewById(R.id.notes_progress)
    }

    override fun update() {
        loadNotes()
    }

    private fun loadNotes() {
        showProgress()
        itemsContainer!!.removeAllViews()
        homeFragment.dataManager.noteService
                .getNotes()
                .subscribe(
                        { r ->
                            hideProgress()
                            r.forEach { createNoteView(it) }
                        },
                        { e ->
                            hideProgress()
                            homeFragment.showError(e.message)
                        }
                )
    }

    private fun createNoteView(note: Note) {
        val view = View.inflate(homeFragment.activity, R.layout.fragment_home_notes_unit_item, null)

        (view.findViewById(R.id.name) as TextView).text = note.description
        view.findViewById(R.id.list_item).setOnClickListener {
            openNote(note.ref)
        }
        view.findViewById(R.id.clear_btn).setOnClickListener {
            deleteNote(note)
        }

        if (itemsContainer!!.childCount > 0) {
            itemsContainer!!.addView(View.inflate(homeFragment.activity, R.layout.horizontal_divider, null))
        }
        itemsContainer!!.addView(view)
    }

    private fun deleteNote(note: Note) {
        showProgress()
        homeFragment.dataManager.noteService
                .deleteNote(note.id)
                .subscribe(
                        {},
                        { e ->
                            hideProgress()
                            homeFragment.showError(e.message)
                        },
                        { loadNotes() }
                )
    }

    private fun openNote(noteRef: String?) {
        if (noteRef == null) return

        if (noteRef.startsWith(Note.TRANSACTION_REF_START)) {
            val parts = noteRef.split("_".toRegex()).dropLastWhile(String::isEmpty).toTypedArray()
            if (parts.size < 2) return

            try {
                val transactionId = java.lang.Long.parseLong(parts[1])
                EditTransactionActivity.actionStart(homeFragment, UiConstants.EDIT_TRANSACTION_REQUEST_CODE, transactionId)
            } catch (ignore: Exception) {
            }

        } else if (noteRef.startsWith(Note.BUDGET_REF_START)) {
            val parts = noteRef.split("_".toRegex()).dropLastWhile(String::isEmpty).toTypedArray()
            if (parts.size < 2) return

            try {
                val budgetPlanId = java.lang.Long.parseLong(parts[1])
                BudgetPlanActivity.actionStart(homeFragment, UiConstants.BUDGET_PLAN_REQUEST_CODE, budgetPlanId)
            } catch (ignore: Exception) {
            }

        } else if (noteRef.startsWith(Note.DEBT_REF_START)) {
            val parts = noteRef.split("_".toRegex()).dropLastWhile(String::isEmpty).toTypedArray()
            if (parts.size < 2) return

            try {
                val debtId = java.lang.Long.parseLong(parts[1])
                DebtActivity.actionStart(homeFragment, UiConstants.DEBT_REQUEST_CODE, debtId)
            } catch (ignore: Exception) {
            }

        } else if (noteRef == Note.SYNC_ERROR_REF) {
            (homeFragment.activity as MainActivity).showSection(MainSection.SYNC)
        }
    }

    private fun showProgress() {
        itemsContainer?.visibility = View.GONE
        itemsProgress?.visibility = View.VISIBLE
    }

    private fun hideProgress() {
        itemsContainer?.visibility = View.VISIBLE
        itemsProgress?.visibility = View.GONE
    }

    override val shortName: String
        get() = UiConstants.NOTES_BLOCK
}
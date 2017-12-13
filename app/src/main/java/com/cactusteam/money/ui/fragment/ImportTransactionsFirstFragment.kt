package com.cactusteam.money.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import com.cactusteam.money.R
import com.cactusteam.money.data.DataUtils
import com.cactusteam.money.data.io.ImportSchema
import com.cactusteam.money.ui.activity.ImportTransactionsActivity

/**
 * @author vpotapenko
 */
class ImportTransactionsFirstFragment : BaseFragment() {

    private var sourceFileView: TextView? = null
    private var errorFileView: TextView? = null
    private var sourceFileDescriptionView: TextView? = null

    private var formatSpinner: Spinner? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_import_transactions_first, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sourceFileView = view.findViewById(R.id.source_file) as TextView
        view.findViewById(R.id.source_file_container).setOnClickListener { sourceFileClicked() }
        sourceFileDescriptionView = view.findViewById(R.id.empty_source_file) as TextView
        errorFileView = view.findViewById(R.id.file_error) as TextView

        updateSourceFileView()

        val adapter = ArrayAdapter(activity, R.layout.fragment_import_transactions_first_format_item, android.R.id.text1, resources.getStringArray(R.array.import_formats))
        adapter.setDropDownViewResource(android.R.layout.simple_list_item_1)
        formatSpinner = view.findViewById(R.id.format) as Spinner
        formatSpinner!!.adapter = adapter
        formatSpinner!!.setSelection(importActivity.format)

        view.findViewById(R.id.next_btn).setOnClickListener { nextClicked() }
    }

    private fun nextClicked() {
        if (importActivity.sourceFile == null) {
            errorFileView!!.visibility = View.VISIBLE
            errorFileView!!.setText(R.string.choose_source_file)
        } else {
            importActivity.format = formatSpinner!!.selectedItemPosition
            doAnalyse()
        }
    }

    private fun doAnalyse() {
        importActivity.showBlockingProgressWithUpdate(getString(R.string.waiting))
        val s = dataManager.systemService
                .analyseImportFile(importActivity.sourceFile!!, importActivity.format)
                .subscribe(
                        { p ->
                            when (p) {
                                is Pair<*, *> -> {
                                    importActivity.updateBlockingProgress(p.first as Int, p.second as Int)
                                }
                                is ImportSchema -> {
                                    importActivity.schema = p
                                }
                            }
                        },
                        { e ->
                            importActivity.hideBlockingProgress()
                            showError(e.message)
                        },
                        {
                            importActivity.hideBlockingProgress()
                            importActivity.nextStep()
                        }
                )
        compositeSubscription.add(s)
    }

    private fun updateSourceFileView() {
        errorFileView!!.visibility = View.GONE
        val sourceFile = importActivity.sourceFile
        if (sourceFile == null) {
            sourceFileView!!.visibility = View.GONE
            sourceFileDescriptionView!!.visibility = View.VISIBLE
        } else {
            sourceFileView!!.visibility = View.VISIBLE
            sourceFileView!!.text = sourceFile.path
            sourceFileDescriptionView!!.visibility = View.GONE
        }
    }

    private fun sourceFileClicked() {
        var fragment: ChooseFileFragment
        val sourceFile = importActivity.sourceFile
        try {
            val exportFolder = DataUtils.exportFolder
            fragment = ChooseFileFragment.build(exportFolder, if (sourceFile == null) "" else sourceFile.name)
        } catch (e: Exception) {
            e.printStackTrace()
            fragment = ChooseFileFragment.build(if (sourceFile == null) "" else sourceFile.name)
        }

        fragment.listener = { file ->
            importActivity.sourceFile = file
            updateSourceFileView()
        }
        fragment.show(fragmentManager, "dialog")
    }

    private val importActivity: ImportTransactionsActivity
        get() = activity as ImportTransactionsActivity

    companion object {

        fun build(): ImportTransactionsFirstFragment {
            return ImportTransactionsFirstFragment()
        }
    }
}

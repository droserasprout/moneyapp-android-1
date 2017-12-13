package com.cactusteam.money.ui.fragment

import android.app.AlertDialog
import android.os.Bundle
import android.os.Environment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.cactusteam.money.R
import com.cactusteam.money.ui.ListItem
import java.io.File
import java.util.*

/**
 * @author vpotapenko
 */
class ChooseFolderFragment : BaseDialogFragment() {

    var listener: ((f: File) -> Unit)? = null

    private var listView: RecyclerView? = null
    private var pathView: TextView? = null

    private var currentDirectory: File? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_choose_folder, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dialog.setTitle(R.string.choose_folder_title)

        listView = view.findViewById(R.id.list) as RecyclerView
        listView!!.layoutManager = LinearLayoutManager(activity)
        listView!!.adapter = ListAdapter()

        initializeProgress(view.findViewById(R.id.progress_bar), listView!!)

        pathView = view.findViewById(R.id.path) as TextView
        view.findViewById(R.id.ok_btn).setOnClickListener { okClicked() }
        view.findViewById(R.id.cancel_btn).setOnClickListener { dismiss() }
        view.findViewById(R.id.add_folder).setOnClickListener { addFolderClicked() }

        if (currentDirectory == null) {
            currentDirectory = Environment.getExternalStorageDirectory()
        }

        updateDirectoryInformation()
    }

    private fun addFolderClicked() {
        val view = View.inflate(activity, R.layout.view_new_name, null)
        val nameEdit = view.findViewById(R.id.name) as EditText
        AlertDialog.Builder(activity).setTitle(R.string.new_folder).setView(view).setPositiveButton(R.string.ok) { dialog, which ->
            val text = nameEdit.text
            if (!TextUtils.isEmpty(text)) addFolder(text.toString())
        }.setNegativeButton(R.string.cancel, null).show()
    }

    private fun addFolder(newName: String) {
        showProgressAsInvisible()
        val s = dataManager.fileService
                .createFolder(currentDirectory, newName)
                .subscribe(
                        {},
                        { e ->
                            hideProgress()
                            showError(e.message)
                        },
                        {
                            hideProgress()
                            updateDirectoryInformation()
                        }
                )
        compositeSubscription.add(s)
    }

    private fun okClicked() {
        if (listener != null) listener!!(currentDirectory!!)

        dismiss()
    }

    private fun updateDirectoryInformation() {
        if (currentDirectory != null) {
            pathView!!.text = currentDirectory!!.path
            showProgressAsInvisible()
            val s = dataManager.fileService
                    .getFolderFiles(currentDirectory!!, true)
                    .subscribe(
                            { r ->
                                hideProgress()
                                showItems(r ?: emptyList<ListItem>())
                            },
                            { e ->
                                hideProgress()
                                showError(e.message)
                            }
                    )
            compositeSubscription.add(s)
        } else {
            pathView!!.text = ""
            showItems(emptyList<ListItem>())
        }
    }

    private fun showItems(items: List<ListItem>) {
        val adapter = listView!!.adapter as ListAdapter
        adapter.items.clear()
        adapter.items.addAll(items)
        adapter.notifyDataSetChanged()

        hideProgress()
    }

    private fun itemClicked(listItem: ListItem) {
        if (listItem.type == ChooseFileFragment.UP_TYPE) {
            moveUp()
        } else if ((listItem.obj as File).isDirectory) {
            currentDirectory = listItem.obj as File?
            updateDirectoryInformation()
        }
    }

    private fun moveUp() {
        val parentFile = currentDirectory!!.parentFile
        if (parentFile == null) {
            val message = getString(R.string.folder_was_not_opened, "null")
            Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
            return
        }

        showProgressAsInvisible()
        val s = dataManager.fileService
                .getFolderFiles(currentDirectory!!, true)
                .subscribe(
                        { r ->
                            hideProgress()
                            if (r == null) {
                                val message = getString(R.string.folder_was_not_opened, parentFile.path)
                                Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
                            } else {
                                currentDirectory = parentFile
                                pathView!!.text = currentDirectory!!.path
                                showItems(r)
                            }
                        },
                        { e ->
                            hideProgress()
                            showError(e.message)
                        }
                )
        compositeSubscription.add(s)
    }

    private inner class ListViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bind(info: ListItem) {
            val iconView = itemView.findViewById(R.id.icon) as ImageView
            val nameView = itemView.findViewById(R.id.name) as TextView

            if (info.type == ChooseFileFragment.UP_TYPE) {
                iconView.visibility = View.VISIBLE
                nameView.text = ".."
            } else {
                iconView.visibility = View.VISIBLE
                nameView.text = (info.obj as File).name
            }

            itemView.findViewById(R.id.list_item).setOnClickListener { itemClicked(info) }
        }
    }

    private inner class ListAdapter : RecyclerView.Adapter<ListViewHolder>() {

        val items = ArrayList<ListItem>()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListViewHolder {
            val v = LayoutInflater.from(activity).inflate(R.layout.fragment_choose_folder_item, parent, false)
            return ListViewHolder(v)
        }

        override fun onBindViewHolder(holder: ListViewHolder, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount(): Int {
            return items.size
        }
    }

    companion object {

        fun build(folder: File): ChooseFolderFragment {
            val fragment = ChooseFolderFragment()
            fragment.currentDirectory = folder
            return fragment
        }
    }

}

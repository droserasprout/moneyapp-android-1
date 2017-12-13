package com.cactusteam.money.data.service

import com.cactusteam.money.data.DataManager
import com.cactusteam.money.ui.ListItem
import com.cactusteam.money.ui.fragment.ChooseFileFragment
import org.apache.commons.io.FileUtils
import java.io.File
import java.util.*

/**
 * @author vpotapenko
 */
abstract class FileInternalService(dataManager: DataManager) : BaseService(dataManager) {

    fun createFolderInternal(parent: File?, name: String) {
        if (parent != null) {
            val newFolder = File(parent, name)
            FileUtils.forceMkdir(newFolder)
        }
    }

    fun getFolderFilesInternal(folder: File, foldersOnly: Boolean = false): List<ListItem>? {
        val files = folder.listFiles() ?: return null

        files.sortWith(Comparator { lhs, rhs ->
            if (lhs.isDirectory && !rhs.isDirectory) {
                -1
            } else if (!lhs.isDirectory && rhs.isDirectory) {
                1
            } else {
                lhs.name.compareTo(rhs.name)
            }
        })

        val items = ArrayList<ListItem>()
        items.add(ListItem(ChooseFileFragment.UP_TYPE, null))
        for (file in files) {
            if (!foldersOnly || file.isDirectory)
                items.add(ListItem(ChooseFileFragment.FILE_TYPE, file))
        }

        return items
    }

    fun deleteFileInternal(file: File) {
        FileUtils.deleteQuietly(file)
    }
}
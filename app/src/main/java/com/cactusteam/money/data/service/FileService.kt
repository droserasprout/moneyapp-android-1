package com.cactusteam.money.data.service

import com.cactusteam.money.data.DataManager
import com.cactusteam.money.ui.ListItem
import rx.Observable
import rx.lang.kotlin.observable
import java.io.File

/**
 * @author vpotapenko
 */
class FileService(dataManager: DataManager) : FileInternalService(dataManager) {

    fun createFolder(parent: File?, name: String): Observable<Unit> {
        val o = observable<Unit> { s ->
            try {
                createFolderInternal(parent, name)
                s.onCompleted()
            } catch (e: Exception) {
                e.printStackTrace()
                s.onError(e)
            }
        }
        return prepareIOObservable(o)
    }

    fun getFolderFiles(folder: File, foldersOnly: Boolean = false): Observable<List<ListItem>?> {
        val o = observable<List<ListItem>?> { s ->
            try {
                s.onNext(getFolderFilesInternal(folder, foldersOnly))
                s.onCompleted()
            } catch (e: Exception) {
                e.printStackTrace()
                s.onError(e)
            }
        }
        return prepareIOObservable(o)
    }

    fun deleteFile(file: File): Observable<Unit> {
        val o = observable<Unit> { s ->
            try {
                deleteFileInternal(file)
                s.onCompleted()
            } catch (e: Exception) {
                e.printStackTrace()
                s.onError(e)
            }
        }
        return prepareIOObservable(o)
    }
}
package com.cactusteam.money.data.service

import com.cactusteam.money.data.DataManager
import com.cactusteam.money.data.dao.Note
import rx.Observable
import rx.lang.kotlin.observable

/**
 * @author vpotapenko
 */
class NoteService(dataManager: DataManager) : NoteInternalService(dataManager) {

    fun deleteNote(id: Long): Observable<Unit> {
        val o = observable<Unit> { s ->
            try {
                deleteNoteInternal(id)
                s.onCompleted()
            } catch (e: Exception) {
                e.printStackTrace()
                s.onError(e)
            }
        }
        return prepareIOObservable(o)
    }

    fun getNotes(): Observable<List<Note>> {
        val o = observable<List<Note>> { s ->
            try {
                s.onNext(getNotesInternal())
                s.onCompleted()
            } catch (e: Exception) {
                e.printStackTrace()
                s.onError(e)
            }
        }
        return prepareIOObservable(o)
    }
}
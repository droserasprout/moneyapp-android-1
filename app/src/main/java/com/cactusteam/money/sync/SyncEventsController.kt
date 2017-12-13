package com.cactusteam.money.sync

import java.util.HashSet

/**
 * @author vpotapenko
 */
class SyncEventsController {

    private val listeners = HashSet<ISyncListener>()

    private var state = IDLE

    val isSyncing: Boolean
        get() = state == SYNCING

    fun addListener(listener: ISyncListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: ISyncListener) {
        listeners.remove(listener)
    }

    fun setState(state: Int) {
        val oldState = this.state
        this.state = state

        if (oldState != state) stateChanged(state)
    }

    fun onProgressUpdated(progress: Int, max: Int) {
        val iterator = listeners.iterator()
        while (iterator.hasNext()) {
            val l = iterator.next()
            l.onProgressUpdated(progress, max)
        }
    }

    private fun stateChanged(newState: Int) {
        if (newState == SYNCING) {
            fireSyncStarted()
        } else if (newState == IDLE) {
            fireSyncFinished()
        }
    }

    private fun fireSyncFinished() {
        val it = listeners.iterator()
        while (it.hasNext()) {
            val l = it.next()
            l.syncFinished()
        }
    }

    private fun fireSyncStarted() {
        val iterator = listeners.iterator()
        while (iterator.hasNext()) {
            val l = iterator.next()
            l.syncStarted()
        }
    }

    companion object {

        val IDLE = 0
        val SYNCING = 1
    }
}

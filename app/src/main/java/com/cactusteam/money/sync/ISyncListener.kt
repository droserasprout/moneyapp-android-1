package com.cactusteam.money.sync

/**
 * @author vpotapenko
 */
interface ISyncListener {

    fun syncStarted()

    fun syncFinished()

    fun onProgressUpdated(progress: Int, max: Int)
}

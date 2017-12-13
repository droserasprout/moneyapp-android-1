package com.cactusteam.money.sync.dropbox

import com.cactusteam.money.sync.changes.ChangesList
import com.cactusteam.money.sync.changes.ChangesListFactory
import com.cactusteam.money.sync.changes.IChangesStorage
import com.dropbox.core.DbxDownloader
import com.dropbox.core.v2.files.FileMetadata
import com.dropbox.core.v2.files.UploadUploader

/**
 * @author vpotapenko
 */
class DropboxLogStorage(private val dropboxClient: DropboxClient) : IChangesStorage {
    private val logFactory = ChangesListFactory()

    override fun initialize() {
        dropboxClient.createFolder(DropboxConstants.SYNC_PATH)
    }

    override fun lock(name: String, lockId: String): Boolean {
        val path = DropboxConstants.SYNC_PATH + "/" + name
        if (dropboxClient.exist(path)) return false

        dropboxClient.writeToFile(path, lockId)
        return true
    }

    override fun unlock(name: String, lockId: String) {
        val path = DropboxConstants.SYNC_PATH + "/" + name
        if (!dropboxClient.exist(path)) return

        val content = dropboxClient.readFromFile(path)
        if (content == lockId) {
            dropboxClient.delete(path)
        }
    }

    override fun getLockId(name: String): String? {
        val path = DropboxConstants.SYNC_PATH + "/" + name
        return if (dropboxClient.exist(path))
            dropboxClient.readFromFile(path)
        else
            null
    }

    override fun saveLog(log: ChangesList, name: String) {
        val path = DropboxConstants.SYNC_PATH + "/" + name

        var uploader: UploadUploader? = null
        try {
            uploader = dropboxClient.startWrite(path)
            logFactory.write(uploader.outputStream, log)
            uploader.finish()
        } finally {
            if (uploader != null) uploader.close()
        }
    }

    override fun getLog(name: String): ChangesList? {
        val path = DropboxConstants.SYNC_PATH + "/" + name
        if (dropboxClient.exist(path)) {
            var downloader: DbxDownloader<FileMetadata>? = null
            try {
                downloader = dropboxClient.startRead(path)
                return logFactory.read(downloader.inputStream)
            } finally {
                if (downloader != null) downloader.close()
            }
        } else {
            return null
        }
    }
}

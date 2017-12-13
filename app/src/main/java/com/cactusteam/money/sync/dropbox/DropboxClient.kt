package com.cactusteam.money.sync.dropbox

import com.dropbox.core.DbxDownloader
import com.dropbox.core.DbxException
import com.dropbox.core.DbxHost
import com.dropbox.core.DbxRequestConfig
import com.dropbox.core.http.OkHttpRequestor
import com.dropbox.core.v2.DbxClientV2
import com.dropbox.core.v2.files.FileMetadata
import com.dropbox.core.v2.files.GetMetadataErrorException
import com.dropbox.core.v2.files.UploadUploader
import com.dropbox.core.v2.files.WriteMode
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.*

/**
 * @author vpotapenko
 */
class DropboxClient(token: String) {

    private val dbxClientV2: DbxClientV2

    init {
        val userLocale = Locale.getDefault().toString()
        val requestConfig = DbxRequestConfig
                .newBuilder("MoneyApp-android/1.0")
                .withUserLocale(userLocale)
                .withHttpRequestor(OkHttpRequestor.INSTANCE)
                .build()
        dbxClientV2 = DbxClientV2(requestConfig, token, DbxHost.DEFAULT)
    }

    fun exist(path: String): Boolean {
        try {
            val metadata = dbxClientV2.files().getMetadata(path)
            return metadata != null
        } catch (e: GetMetadataErrorException) {
            return false
        }

    }

    fun readFromFile(path: String): String {
        val out = ByteArrayOutputStream()
        dbxClientV2.files().download(path).download(out)
        out.close()

        return out.toString()
    }

    fun startRead(path: String): DbxDownloader<FileMetadata> {
        return dbxClientV2.files().download(path)
    }

    fun startWrite(path: String): UploadUploader {
        return dbxClientV2.files().uploadBuilder(path).withMode(WriteMode.OVERWRITE).start()
    }

    fun writeToFile(path: String, content: String) {
        val `in` = ByteArrayInputStream(content.toByteArray())

        dbxClientV2.files().uploadBuilder(path).withMode(WriteMode.OVERWRITE).start().uploadAndFinish(`in`)
        `in`.close()
    }

    fun createFolder(path: String) {
        dbxClientV2.files().createFolder(path)
    }

    fun delete(path: String) {
        dbxClientV2.files().delete(path)
    }
}

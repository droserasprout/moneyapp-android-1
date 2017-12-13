package com.cactusteam.money.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.FileDescriptor
import java.io.IOException
import java.io.InputStream

/**
 * @author vpotapenko
 */
object BitmapUtils {

    /**
     * Decode and sample down a bitmap from a file input stream to the requested width and height.

     * @param fileDescriptor The file descriptor to read from
     * *
     * @param reqWidth       The requested width of the resulting bitmap
     * *
     * @param reqHeight      The requested height of the resulting bitmap
     * *
     * @return A bitmap sampled down from the original with the same aspect ratio and dimensions
     * * that are equal to or greater than the requested width and height
     */
    fun decodeBitmapFromDescriptor(fileDescriptor: FileDescriptor, reqWidth: Int, reqHeight: Int): Bitmap {

        // First decode with inJustDecodeBounds=true to check dimensions
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeFileDescriptor(fileDescriptor, null, options)

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false
        return BitmapFactory.decodeFileDescriptor(fileDescriptor, null, options)
    }

    fun decodeBitmapFromFile(path: String, reqWidth: Int, reqHeight: Int): Bitmap {
        // First decode with inJustDecodeBounds=true to check dimensions
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeFile(path, options)

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false
        return BitmapFactory.decodeFile(path, options)

    }

    @Throws(IOException::class)
    fun decodeBitmapFromInputStream(inputStream: InputStream, reqWidth: Int, reqHeight: Int): Bitmap {
        // First decode with inJustDecodeBounds=true to check dimensions
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeStream(inputStream, null, options)

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false
        inputStream.reset()

        return BitmapFactory.decodeStream(inputStream, null, options)
    }

    private fun calculateInSampleSize(
            options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        // Raw height and width of image
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {

            val halfHeight = height / 2
            val halfWidth = width / 2

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while (halfHeight / inSampleSize > reqHeight && halfWidth / inSampleSize > reqWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }
}

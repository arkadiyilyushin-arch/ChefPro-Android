package com.chefpro.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream
import java.io.File

class PhotoStorage(context: Context) {

    private val photosDir = File(context.applicationContext.filesDir, PHOTOS_DIR_NAME).apply {
        mkdirs()
    }

    fun saveDishPhoto(dishId: String, imageBytes: ByteArray): String {
        val filename = "dish_$dishId.jpg"
        writePhoto(filename, imageBytes)
        return filename
    }

    fun loadDishPhoto(filename: String): ByteArray? = loadPhoto(filename)

    fun deleteDishPhoto(filename: String) {
        deletePhoto(filename)
    }

    fun saveStepPhoto(stepId: String, imageBytes: ByteArray): String {
        val filename = "step_$stepId.jpg"
        writePhoto(filename, imageBytes)
        return filename
    }

    fun loadStepPhoto(filename: String): ByteArray? = loadPhoto(filename)

    fun deleteStepPhoto(filename: String) {
        deletePhoto(filename)
    }

    fun photoFile(filename: String): File = File(photosDir, filename)

    private fun writePhoto(filename: String, imageBytes: ByteArray) {
        val resized = resizeImage(imageBytes)
        File(photosDir, filename).writeBytes(resized)
    }

    private fun loadPhoto(filename: String): ByteArray? {
        val file = File(photosDir, filename)
        return if (file.exists()) file.readBytes() else null
    }

    private fun deletePhoto(filename: String) {
        File(photosDir, filename).delete()
    }

    private fun resizeImage(
        data: ByteArray,
        maxDimension: Int = MAX_DIMENSION,
        quality: Int = JPEG_QUALITY,
    ): ByteArray {
        val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size) ?: return data
        val width = bitmap.width
        val height = bitmap.height

        val scaledBitmap = if (width <= maxDimension && height <= maxDimension) {
            bitmap
        } else {
            val ratio = maxDimension.toFloat() / maxOf(width, height)
            val newWidth = (width * ratio).toInt().coerceAtLeast(1)
            val newHeight = (height * ratio).toInt().coerceAtLeast(1)
            Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true).also {
                bitmap.recycle()
            }
        }

        return ByteArrayOutputStream().use { stream ->
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
            scaledBitmap.recycle()
            stream.toByteArray()
        }
    }

    companion object {
        private const val PHOTOS_DIR_NAME = "photos"
        private const val MAX_DIMENSION = 1024
        private const val JPEG_QUALITY = 80
    }
}

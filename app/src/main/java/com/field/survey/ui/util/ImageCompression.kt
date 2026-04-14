package com.field.survey.ui.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.util.Base64
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayOutputStream
import java.io.File

object ImageCompression {

    private const val MAX_DIMENSION_PX = 1600
    private const val JPEG_QUALITY = 80

    /**
     * Reads a JPEG from disk, downscales to ≤1600px on the long side, applies EXIF rotation,
     * re-encodes at JPEG quality 80, returns a Base64 string.
     *
     * Designed to keep the resulting Firestore document payload well under the 1 MB cap.
     * Returns null on any failure.
     */
    fun fileToCompressedBase64(path: String): String? {
        val file = File(path)
        if (!file.exists() || file.length() == 0L) return null

        return runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(path, bounds)
            val (srcW, srcH) = bounds.outWidth to bounds.outHeight
            if (srcW <= 0 || srcH <= 0) return null

            val sampleSize = computeInSampleSize(srcW, srcH, MAX_DIMENSION_PX)
            val decoded =
                BitmapFactory.decodeFile(
                    path,
                    BitmapFactory.Options().apply { inSampleSize = sampleSize },
                ) ?: return null

            val rotated = applyExifRotation(path, decoded)
            val scaled = scaleIfNeeded(rotated)

            val baos = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, baos)
            scaled.recycle()

            Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
        }.getOrNull()
    }

    private fun computeInSampleSize(
        width: Int,
        height: Int,
        targetMax: Int,
    ): Int {
        var sample = 1
        val longest = maxOf(width, height)
        while (longest / sample > targetMax * 2) {
            sample *= 2
        }
        return sample
    }

    private fun scaleIfNeeded(bitmap: Bitmap): Bitmap {
        val longest = maxOf(bitmap.width, bitmap.height)
        if (longest <= MAX_DIMENSION_PX) return bitmap
        val ratio = MAX_DIMENSION_PX.toFloat() / longest
        val newW = (bitmap.width * ratio).toInt()
        val newH = (bitmap.height * ratio).toInt()
        val scaled = Bitmap.createScaledBitmap(bitmap, newW, newH, true)
        if (scaled !== bitmap) bitmap.recycle()
        return scaled
    }

    private fun applyExifRotation(
        path: String,
        source: Bitmap,
    ): Bitmap {
        val exif = runCatching { ExifInterface(path) }.getOrNull() ?: return source
        val orientation =
            exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
            else -> return source
        }
        val rotated = Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
        if (rotated !== source) source.recycle()
        return rotated
    }
}

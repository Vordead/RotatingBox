package com.example.rotatingbox.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.util.SparseIntArray
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.Color
import androidx.core.util.set

object BitmapUtil {

    /**
     * Finds the dominant color in a bitmap.
     *
     * @param bitmap The input bitmap.
     * @return The dominant color as a Color object.
     */
    fun findDominantColor(bitmap: Bitmap): Color {
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 128, 128, false)
        val colorFrequencyMap = SparseIntArray()

        var dominantColor = 0
        var maxCount = 0

        (0 until scaledBitmap.width).forEach { x ->
            (0 until scaledBitmap.height).forEach { y ->
                val pixelColor = bucketColor(scaledBitmap.getPixel(x, y))
                val count = colorFrequencyMap[pixelColor] + 1
                colorFrequencyMap[pixelColor] = count
                if (count > maxCount) {
                    maxCount = count
                    dominantColor = pixelColor
                }
            }
        }

        return Color(dominantColor)
    }

    /**
     * Converts a URI to a Bitmap.
     *
     * @param context The context.
     * @param uri The URI of the image.
     * @return The Bitmap, or null if conversion fails.
     * @throws BitmapConversionException if an error occurs during conversion.
     */
    @RequiresApi(Build.VERSION_CODES.P)
    fun uriToBitmap(context: Context, uri: Uri): Bitmap {
        return try {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            val bitmap = ImageDecoder.decodeBitmap(source)
            bitmap.copy(Bitmap.Config.ARGB_8888, true)
        } catch (e: Exception) {
            throw BitmapConversionException("Failed to convert URI to Bitmap", e)
        }
    }

    /**
     * Buckets a color to reduce the number of distinct colors.
     *
     * @param color The input color.
     * @return The bucketed color.
     */
    private fun bucketColor(color: Int): Int {
        val red = (color shr 16 and 0xFF) / 32 * 32
        val green = (color shr 8 and 0xFF) / 32 * 32
        val blue = (color and 0xFF) / 32 * 32
        return (0xFF shl 24) or (red shl 16) or (green shl 8) or blue
    }
}

class BitmapConversionException(message: String, cause: Throwable) : Exception(message, cause)

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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

object BitmapUtil {

    //Step 1
    fun findDominantColorHashMap(bitmap: Bitmap, useDownscaling: Boolean = false): Color {
        val scaledBitmap = if(useDownscaling) Bitmap.createScaledBitmap(bitmap, bitmap.width/4, bitmap.height/4, false) else bitmap

        val colorCountMap = mutableMapOf<Int, Int>()

        for (x in 0 until scaledBitmap.width) {
            for (y in 0 until scaledBitmap.height) {
                val pixelColor = scaledBitmap.getPixel(x, y)
                colorCountMap[pixelColor] = colorCountMap.getOrDefault(pixelColor, 0) + 1
            }
        }

        var dominantColor = 0
        var maxCount = 0
        for ((color, count) in colorCountMap) {
            if (count > maxCount) {
                maxCount = count
                dominantColor = color
            }
        }

        return Color(dominantColor)
    }

    //Step 2
    fun findDominantColorHashMapPretty(bitmap: Bitmap, useDownscaling : Boolean = false): Color {
        val scaledBitmap = if(useDownscaling) Bitmap.createScaledBitmap(bitmap, bitmap.width/4, bitmap.height/4, false) else bitmap
        val colorCountMap = HashMap<Int, Int>()
        for (x in 0 until scaledBitmap.width) {
            for (y in 0 until scaledBitmap.height) {
                val pixelColor = scaledBitmap.getPixel(x, y)
                colorCountMap[pixelColor] = colorCountMap.getOrDefault(pixelColor, 0) + 1
            }
        }
        val dominantColor = colorCountMap.maxByOrNull { it.value }?.key
        return Color(dominantColor ?: 0xFFFFFFF)
    }

    //Step 3
    fun findDominantColorSparseIntArray(bitmap: Bitmap, useDownscaling: Boolean = false): Color {
        val scaledBitmap = if(useDownscaling) Bitmap.createScaledBitmap(bitmap, bitmap.width/4, bitmap.height/4, false) else bitmap

        val colorCountMap = SparseIntArray()

        for (x in 0 until scaledBitmap.width) {
            for (y in 0 until scaledBitmap.height) {
                val pixelColor = scaledBitmap.getPixel(x, y)
                colorCountMap[pixelColor] = colorCountMap.get(pixelColor, 0) + 1
            }
        }

        var dominantColor = 0
        var maxCount = 0
        for (i in 0 until colorCountMap.size()) {
            if (colorCountMap.valueAt(i) > maxCount) {
                maxCount = colorCountMap.valueAt(i)
                dominantColor = colorCountMap.keyAt(i)
            }
        }

        return Color(dominantColor)
    }

    //Step 4

    /**
     * Finds the dominant color in a bitmap.
     *
     * @param bitmap The input bitmap.
     * @return The dominant color as a Color object.
     */
    fun findDominantColorSparseIntArrayPretty(bitmap: Bitmap, useDownscaling: Boolean = false): Color {
        val scaledBitmap = if(useDownscaling) Bitmap.createScaledBitmap(bitmap, bitmap.width/4, bitmap.height/4, false) else bitmap
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

    //Step 5
     suspend fun findDominantColorParallelism(bitmap: Bitmap, useDownscaling: Boolean = false): Color = withContext(Dispatchers.Default) {
        val scaledBitmap = if(useDownscaling) Bitmap.createScaledBitmap(bitmap, bitmap.width/4, bitmap.height/4, false) else bitmap
        val colorCountMap = ConcurrentHashMap<Int, Int>()
        val cores = Runtime.getRuntime().availableProcessors()
        val chunkSize = scaledBitmap.height / cores

        val jobs = (0 until cores).map { core ->
            async(Dispatchers.Default) {
                val startY = core * chunkSize
                val endY = if (core == cores - 1) scaledBitmap.height else (core + 1) * chunkSize
                for (x in 0 until scaledBitmap.width) {
                    for (y in startY until endY) {
                        val pixelColor = scaledBitmap.getPixel(x, y)
                        colorCountMap.merge(pixelColor, 1, Int::plus)
                    }
                }
            }
        }

        jobs.forEach { it.await() }

        var dominantColor = 0
        var maxCount = 0
        for ((color, count) in colorCountMap) {
            if (count > maxCount) {
                maxCount = count
                dominantColor = color
            }
        }

        return@withContext Color(dominantColor)
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

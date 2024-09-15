package com.example.rotatingbox

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.rotatingbox.util.BitmapUtil
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.logging.FileHandler
import java.util.logging.Logger
import java.util.logging.SimpleFormatter
import kotlin.system.measureTimeMillis

@RunWith(AndroidJUnit4::class)
class BitmapUtilTest {

    private val logger = Logger.getLogger("BitmapUtilTest")

    init {
        val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.filesDir, "bitmap_util_test.log")
        if (!file.exists()) {
            file.createNewFile() // Create the file if it doesn't exist
        }
        val fileHandler = FileHandler(file.absolutePath)
        fileHandler.formatter = SimpleFormatter()
        logger.addHandler(fileHandler)
    }

    @Test
    fun testFindDominantColor() = runTest {
        val smallBitmap = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888)
        val largeBitmap = Bitmap.createBitmap(1024, 1024, Bitmap.Config.ARGB_8888)

        val functions = listOf(
            "findDominantColorHashMap" to { bitmap: Bitmap, downscale: Boolean ->
                BitmapUtil.findDominantColorHashMap(bitmap, downscale)
            },
            "findDominantColorSparseIntArray" to { bitmap: Bitmap, downscale: Boolean ->
                BitmapUtil.findDominantColorSparseIntArray(bitmap, downscale)
            },
            "findDominantColorParallelism" to { bitmap: Bitmap, downscale: Boolean ->
                runBlocking {
                    BitmapUtil.findDominantColorParallelism(bitmap, downscale)
                }
            }
        )

        val testCases = listOf(
            TestCase(smallBitmap, true, "Small Bitmap (Downscaled)"),
            TestCase(smallBitmap, false, "Small Bitmap (No Downscaling)"),
            TestCase(largeBitmap, true, "Large Bitmap (Downscaled)"),
            TestCase(largeBitmap, false, "Large Bitmap (No Downscaling)")
        )

        for (testCase in testCases) {
            for ((name, function) in functions) {
                val times = mutableListOf<Long>()

                repeat(10) { // Run each test case 10 times
                    val time = measureTimeMillis {
                        function(testCase.bitmap, testCase.downscale)
                    }
                    times.add(time)
                }

                val averageTime = times.average()
                val logMessage = "${testCase.description} - $name: Average time: $averageTime ms"
                logger.info(logMessage)
            }
        }
    }

    data class TestCase(val bitmap: Bitmap, val downscale: Boolean, val description: String)
}
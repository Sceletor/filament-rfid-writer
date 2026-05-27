package com.blumlaut.filamenttagwriter

import android.graphics.Bitmap
import android.util.Log
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.blumlaut.filamenttagwriter.data.local.FilamentEntity
import com.blumlaut.filamenttagwriter.data.model.generateProductionDateRaw
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.Base64
import java.util.UUID

/**
 * Instrumented test that captures screenshots of each app screen.
 *
 * Run with:
 *   ./gradlew :app:connectedAndroidTest \
 *     -PtestInstrumentationRunnerArguments.class=com.blumlaut.filamenttagwriter.ScreenshotTest
 *
 * Screenshots are saved to the app's cache directory as PNGs.
 * Pull them with:
 *   adb exec-out run-as com.blumlaut.filamenttagwriter cat cache/screenshot_<screen>.png > <local-path>.png
 */
class ScreenshotTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private val testFilaments = listOf(
        FilamentEntity(
            id = UUID.randomUUID().toString(),
            name = "PLA Black",
            manufacturerCode = 0xEEEEEEEE.toInt(),
            material = "PLA",
            subtypeCode = 0x0000,
            subtype = "PLA",
            colorRgb = 0x000000,
            colorModifier = 0x00,
            minTemp = 190,
            maxTemp = 230,
            diameter = 1.75f,
            weight = 1000,
            productionDateRaw = generateProductionDateRaw(),
        ),
        FilamentEntity(
            id = UUID.randomUUID().toString(),
            name = "PLA Turquoise",
            manufacturerCode = 0xEEEEEEEE.toInt(),
            material = "PLA",
            subtypeCode = 0x0000,
            subtype = "PLA",
            colorRgb = 0x44F1FF,
            colorModifier = 0x00,
            minTemp = 190,
            maxTemp = 230,
            diameter = 1.75f,
            weight = 1000,
            productionDateRaw = generateProductionDateRaw(),
        ),
        FilamentEntity(
            id = UUID.randomUUID().toString(),
            name = "PLA White",
            manufacturerCode = 0xEEEEEEEE.toInt(),
            material = "PLA",
            subtypeCode = 0x0000,
            subtype = "PLA",
            colorRgb = 0xFFFFFF,
            colorModifier = 0x00,
            minTemp = 190,
            maxTemp = 230,
            diameter = 1.75f,
            weight = 1000,
            productionDateRaw = generateProductionDateRaw(),
        ),
        FilamentEntity(
            id = UUID.randomUUID().toString(),
            name = "PLA Orange",
            manufacturerCode = 0xEEEEEEEE.toInt(),
            material = "PLA",
            subtypeCode = 0x0000,
            subtype = "PLA",
            colorRgb = 0xFF8C00,
            colorModifier = 0x00,
            minTemp = 190,
            maxTemp = 230,
            diameter = 1.75f,
            weight = 1000,
            productionDateRaw = generateProductionDateRaw(),
        ),
        FilamentEntity(
            id = UUID.randomUUID().toString(),
            name = "PLA Blue",
            manufacturerCode = 0xEEEEEEEE.toInt(),
            material = "PLA",
            subtypeCode = 0x0000,
            subtype = "PLA",
            colorRgb = 0x2277FF,
            colorModifier = 0x00,
            minTemp = 190,
            maxTemp = 230,
            diameter = 1.75f,
            weight = 1000,
            productionDateRaw = generateProductionDateRaw(),
        ),
    )

    @Test
    fun captureAllScreens() {
        val activity = composeTestRule.activity
        val cacheDir = activity.cacheDir
        Log.d("ScreenshotTest", "Cache dir: $cacheDir")

        // Seed test filaments directly into the Room database
        val dao = (activity.application as FilamentTagApp).database.filamentDao()
        runBlocking {
            testFilaments.forEach { dao.insert(it) }
        }

        // Wait for the ViewModel to receive the data via Flow
        Thread.sleep(1000)

        // 1. Capture Read screen (default start destination)
        composeTestRule.onRoot().captureToImage().saveAs("read")

        // 2. Navigate to Catalog and capture
        composeTestRule.onNodeWithContentDescription("Catalog", useUnmergedTree = true).performClick()
        Thread.sleep(800)
        composeTestRule.onRoot().captureToImage().saveAs("catalog")

        // 3. Navigate to Printer and capture
        composeTestRule.onNodeWithContentDescription("Printer", useUnmergedTree = true).performClick()
        Thread.sleep(800)
        composeTestRule.onRoot().captureToImage().saveAs("printer")

        // 4. Navigate back to Catalog, then open the form via FAB
        composeTestRule.onNodeWithContentDescription("Catalog", useUnmergedTree = true).performClick()
        Thread.sleep(800)
        composeTestRule.onNodeWithContentDescription("Add Filament", useUnmergedTree = true).performClick()
        Thread.sleep(800)
        composeTestRule.onRoot().captureToImage().saveAs("form")
    }

    private fun ImageBitmap.saveAs(tag: String) {
        val bitmap = asAndroidBitmap()
        try {
            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
            val bytes = baos.toByteArray()
            val base64 = Base64.getEncoder().encodeToString(bytes)
            Log.d("ScreenshotTest", "===SCREENSHOT_START:$tag===")
            // Log base64 in chunks to avoid logcat line length limits (approx 1000 chars per line)
            val chunkSize = 900
            for (i in (0 until base64.length step chunkSize)) {
                Log.d("ScreenshotTest", base64.substring(i, minOf(i + chunkSize, base64.length)))
            }
            Log.d("ScreenshotTest", "===SCREENSHOT_END:$tag===")
            Log.d("ScreenshotTest", "Screenshot $tag: ${bytes.size} bytes")
        } catch (e: Exception) {
            Log.e("ScreenshotTest", "Failed to capture $tag", e)
            throw e
        } finally {
            bitmap.recycle()
        }
    }
}

package com.blumlaut.filamenttagwriter

import android.content.ContentValues
import android.graphics.Bitmap
import android.provider.MediaStore
import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.blumlaut.filamenttagwriter.data.local.FilamentEntity
import com.blumlaut.filamenttagwriter.data.local.PrinterEntity
import com.blumlaut.filamenttagwriter.data.model.*
import com.blumlaut.filamenttagwriter.network.ConnectionState
import com.blumlaut.filamenttagwriter.nfc.NfcReaderWriter
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import java.util.UUID

/**
 * Instrumented test that captures screenshots of each app screen.
 * Writes PNG files to DCIM/Screenshots via MediaStore (survives teardown).
 *
 * Run via script:
 *   ./scripts/capture-screenshots.sh
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
        val theme = InstrumentationRegistry.getArguments().getString("theme", "dark")
        val suffix = "_${theme}"
        Log.d("ScreenshotTest", "Theme: $theme, suffix: $suffix")

        val activity = composeTestRule.activity
        val app = activity.application as FilamentTagApp

        // Seed test filaments directly into the Room database
        val filamentDao = app.database.filamentDao()
        runBlocking {
            testFilaments.forEach { filamentDao.insert(it) }
        }

        // Seed a demo printer with a known ID
        val printerId = "printer-demo-001"
        val printerDao = app.database.printerDao()
        runBlocking {
            printerDao.insert(
                PrinterEntity(
                    id = printerId,
                    name = "Centauri Carbon",
                    ipAddress = "192.168.1.100",
                    port = 3030,
                    lastSeen = System.currentTimeMillis(),
                )
            )
        }

        // Get the FilamentViewModel to set simulated NFC data
        val filamentViewModel = try {
            val store = (activity as androidx.activity.ComponentActivity).viewModelStore
            val vmKey = store.keys().find { it.contains("FilamentViewModel") } ?: store.keys().first()
            store[vmKey] as FilamentViewModel
        } catch (e: Exception) {
            Log.e("ScreenshotTest", "Could not access FilamentViewModel", e)
            null
        }

        // Navigate to Printer first to trigger PrinterViewModel creation in MainActivity
        composeTestRule.onNodeWithContentDescription("Printer", useUnmergedTree = true).performClick()
        Thread.sleep(1000)

        // Access MainActivity's PrinterViewModel via test accessor
        val printerViewModel = activity.getPrinterViewModelForTesting()

        // Seed simulated NFC read result
        filamentViewModel?.let {
            val demoFilament = Filament(
                id = UUID.randomUUID().toString(),
                name = "ELEGOO PLA Silk Turquoise",
                manufacturerCode = 0xEEEEEEEE.toInt(),
                material = "PLA",
                subtypeCode = 0x0000,
                subtype = "PLA Silk",
                color = "#44F1FF",
                colorRgb = 0x44F1FF,
                colorModifier = 0x00,
                minTemp = 195,
                maxTemp = 230,
                diameter = 1.75f,
                weight = 1000,
                productionDateRaw = generateProductionDateRaw(),
            )
            it.nfcReadResult.value = NfcReaderWriter.ReadResult.Success(demoFilament)
            Log.d("ScreenshotTest", "Seeded simulated NFC read result")
        }

        // Inject dummy printer data
        printerViewModel?.let { vm ->
            try {
                val trays = listOf(
                    CanvasTray(
                        trayId = 0,
                        brand = "ELEGOO",
                        filamentType = "PLA",
                        filamentName = "PLA Silk Turquoise",
                        filamentCode = "0xA1B2C3",
                        filamentColor = "#44F1FF",
                        minNozzleTemp = 195,
                        maxNozzleTemp = 230,
                        status = 1,
                    ),
                    CanvasTray(
                        trayId = 1,
                        brand = "Prusament",
                        filamentType = "PETG",
                        filamentName = "PETG Black",
                        filamentCode = "0xD4E5F6",
                        filamentColor = "#000000",
                        minNozzleTemp = 220,
                        maxNozzleTemp = 260,
                        status = 1,
                    ),
                    CanvasTray(
                        trayId = 2,
                        brand = "ELEGOO",
                        filamentType = "PLA",
                        filamentName = "PLA Matte White",
                        filamentCode = "0x112233",
                        filamentColor = "#FFFFFF",
                        minNozzleTemp = 190,
                        maxNozzleTemp = 230,
                        status = 1,
                    ),
                    CanvasTray(
                        trayId = 3,
                        brand = "Hatchbox",
                        filamentType = "ABS",
                        filamentName = "ABS Orange",
                        filamentCode = "",
                        filamentColor = "#FF8C00",
                        minNozzleTemp = 230,
                        maxNozzleTemp = 260,
                        status = 1,
                    ),
                )
                val materialData = CanvasMaterialData(
                    activeCanvasId = 0,
                    activeTrayId = 0,
                    autoRefill = 0,
                    canvasList = listOf(
                        CanvasUnit(
                            canvasId = 0,
                            connected = true,
                            trayList = trays,
                        )
                    ),
                )
                val printerStatus = PrinterStatus(
                    currentStatusCodes = listOf(1),
                    timeLapseStatus = 0,
                    platformType = 1,
                    amsConnectStatus = 1,
                    tempNozzle = 210f,
                    tempNozzleTarget = 215f,
                    tempHotbed = 60f,
                    tempHotbedTarget = 60f,
                    tempBox = 35f,
                    tempBoxTarget = 35f,
                    currentCoord = "120.50,80.30,45.20",
                    fanModel = 80,
                    fanAuxiliary = 50,
                    fanBox = 30,
                    zOffset = 0.05f,
                    printInfo = PrintInfo(
                        status = 1, // Printing
                        currentLayer = 142,
                        totalLayer = 318,
                        currentTicks = 4560,
                        totalTicks = 10200,
                        filename = "/sdcard/prints/dragon_vase_200mm.gcode",
                        taskId = "task-abc-123",
                        printSpeedPct = 100,
                        progress = 45,
                    ),
                )
                val attributes = PrinterAttributes(
                    name = "Centauri Carbon",
                    machineName = "ELEGOO-Centauri-Carbon",
                    brandName = "ELEGOO",
                    protocolVersion = "1.0.0",
                    firmwareVersion = "1.3.2",
                    xyzSize = "300x300x350",
                    mainboardIP = "192.168.1.100",
                    mainboardMAC = "AA:BB:CC:DD:EE:FF",
                    mainboardId = "main-001",
                    networkStatus = "connected",
                    usbDiskStatus = 1,
                    capabilities = listOf("ams", "camera", "timelapse"),
                    supportFileType = listOf("gcode", "gco"),
                    cameraStatus = 1,
                    remainingMemory = 5368709120L, // 5GB
                    devicesStatus = DevicesStatus(
                        sgStatus = 1,
                        zMotorStatus = 1,
                        xMotorStatus = 1,
                        yMotorStatus = 1,
                    ),
                )
                vm.injectTestData(
                    printerId = printerId,
                    connectionState = ConnectionState.Connected,
                    materialData = materialData,
                    status = printerStatus,
                    attributes = attributes,
                )
                Log.d("ScreenshotTest", "Seeded dummy printer data")
            } catch (e: Exception) {
                Log.e("ScreenshotTest", "Failed to seed dummy printer data", e)
            }
        }

        // Wait for catalog to populate and UI to recompose
        Thread.sleep(1500)
        composeTestRule.onRoot().assertIsDisplayed()

        // 3. Capture Printer screen first (we're already on it, dummy data injected above)
        Thread.sleep(500)
        composeTestRule.onRoot().captureToImage().saveAs("printer$suffix")

        // Navigate back to Read
        composeTestRule.onNodeWithContentDescription("Read", useUnmergedTree = true).performClick()
        Thread.sleep(800)

        // 1. Capture Read screen (shows simulated NFC read result, no NFC error)
        composeTestRule.onRoot().captureToImage().saveAs("read$suffix")

        // 2. Navigate to Catalog and capture
        composeTestRule.onNodeWithContentDescription("Catalog", useUnmergedTree = true).performClick()
        Thread.sleep(800)
        composeTestRule.onRoot().captureToImage().saveAs("catalog$suffix")

        // 4. Navigate to Write tab (shows Write screen with NFC ready state)
        composeTestRule.onNodeWithContentDescription("Write", useUnmergedTree = true).performClick()
        Thread.sleep(800)
        composeTestRule.onRoot().captureToImage().saveAs("write$suffix")

        // 5. Navigate back to Catalog, then open the form via FAB
        composeTestRule.onNodeWithContentDescription("Catalog", useUnmergedTree = true).performClick()
        Thread.sleep(800)
        composeTestRule.onNodeWithContentDescription("Add Filament", useUnmergedTree = true).performClick()
        Thread.sleep(800)
        composeTestRule.onRoot().captureToImage().saveAs("form$suffix")
    }

    private fun ImageBitmap.saveAs(tag: String) {
        val bitmap = asAndroidBitmap()
        try {
            val context = composeTestRule.activity
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "screenshot_${tag}.png")
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/Screenshots")
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: throw Exception("Failed to create MediaStore entry")
            resolver.openOutputStream(uri)?.use { os ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, os)
                os.flush()
            }
            Log.d("ScreenshotTest", "Screenshot $tag saved to ${uri.path}")
        } catch (e: Exception) {
            Log.e("ScreenshotTest", "Failed to capture $tag", e)
            throw e
        } finally {
            bitmap.recycle()
        }
    }
}

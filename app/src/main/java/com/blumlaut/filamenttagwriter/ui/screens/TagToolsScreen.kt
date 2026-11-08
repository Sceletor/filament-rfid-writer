package com.blumlaut.filamenttagwriter.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.blumlaut.filamenttagwriter.FilamentViewModel
import com.blumlaut.filamenttagwriter.TagToolMode
import com.blumlaut.filamenttagwriter.data.model.FilamentCatalogBackup

/** NFC tools always require explicit arming before any tag is touched. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagToolsScreen(viewModel: FilamentViewModel, nfcAvailable: Boolean, nfcEnabled: Boolean) {
    val context = LocalContext.current
    val exportFile = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            val result = runCatching {
                val stream = context.contentResolver.openOutputStream(uri)
                    ?: error("Cannot open output document")
                stream.bufferedWriter(Charsets.UTF_8).use {
                    it.write(FilamentCatalogBackup.encode(viewModel.catalog.value))
                }
                viewModel.catalog.value.size
            }
            viewModel.backupResult.value = result.fold(
                onSuccess = { "Exported " + it + " profiles to JSON." },
                onFailure = { "Export failed: " + it.message },
            )
        }
    }
    val importFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val result = runCatching {
                val stream = context.contentResolver.openInputStream(uri)
                    ?: error("Cannot read chosen document")
                stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            }
            result.fold(
                onSuccess = { viewModel.importCatalogBackup(it) },
                onFailure = { viewModel.backupResult.value = "Import failed: " + it.message },
            )
        }
    }
    var confirmErase by remember { mutableStateOf(false) }
    val mode = viewModel.tagToolMode.value
    val busy = viewModel.tagToolBusy.value

    Scaffold(topBar = { TopAppBar(title = { Text("CC2X NFC Tools") }) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("Auto-read", style = MaterialTheme.typography.titleMedium)
            Text("NFC reading already starts automatically on the Read tab. Enable this to read while browsing other tabs.",
                style = MaterialTheme.typography.bodySmall)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Auto-read across tabs")
                Switch(checked = viewModel.autoReadEverywhere.value,
                    onCheckedChange = { viewModel.setAutoReadEverywhere(context, it) })
            }

            HorizontalDivider()
            Text("Catalog backup", style = MaterialTheme.typography.titleMedium)
            Text("Export all filament profiles to JSON or import a CC2X backup. Import adds new records without deleting the existing catalog.",
                style = MaterialTheme.typography.bodySmall)
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = { exportFile.launch("cc2x-filaments.json") },
            ) { Text("Export App Database (JSON)") }
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = { importFile.launch(arrayOf("application/json", "text/plain")) },
            ) { Text("Import App Database (JSON)") }
            val backupStatus = viewModel.backupResult.value
            if (!backupStatus.isNullOrBlank()) Text(backupStatus, style = MaterialTheme.typography.bodySmall)

            HorizontalDivider()
            Text("Memory viewer", style = MaterialTheme.typography.titleMedium)
            Text("Read NFC pages 0..39 in HEX; nothing is written.", style = MaterialTheme.typography.bodySmall)
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                enabled = nfcAvailable && nfcEnabled && !busy,
                onClick = {
                    viewModel.tagToolResult.value = ""
                    viewModel.tagToolMode.value = TagToolMode.READ_MEMORY
                },
            ) { Text("Read Tag Memory") }

            HorizontalDivider()
            Text("Clear filament data", style = MaterialTheme.typography.titleMedium)
            Text("TEST TAGS ONLY. Erases filament data pages 16..39, NOT the UID, NDEF URL, lock bits, password or NFC configuration. Tag will need to be reprogrammed.",
                style = MaterialTheme.typography.bodySmall)
            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = nfcAvailable && nfcEnabled && !busy,
                onClick = { confirmErase = true },
            ) { Text("Clear Tag Filament Profile…") }

            if (mode != TagToolMode.IDLE) {
                Text(
                    if (mode == TagToolMode.READ_MEMORY) "Hold the tag near phone for read-only memory dump."
                    else "Erase ARMED: present a spare TEST tag to clear its filament data.",
                    color = MaterialTheme.colorScheme.primary,
                )
                TextButton(onClick = { viewModel.tagToolMode.value = TagToolMode.IDLE }) {
                    Text("Cancel NFC action")
                }
            }
            if (busy) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            val result = viewModel.tagToolResult.value
            if (!result.isNullOrBlank()) {
                SelectionContainer {
                    Text(result, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
    if (confirmErase) {
        AlertDialog(
            onDismissRequest = { confirmErase = false },
            title = { Text("Erase filament data on a TEST tag?") },
            text = { Text("This removes the filament profile stored in NFC pages 16–39. It cannot be undone; a previously recognized spool will need to be reprogrammed. Original UID, NDEF and security configuration are preserved. After confirmation, tap the test tag to begin.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmErase = false
                    viewModel.tagToolResult.value = ""
                    viewModel.tagToolMode.value = TagToolMode.CLEAR_FILAMENT
                }) { Text("I understand — arm erase") }
            },
            dismissButton = { TextButton(onClick = { confirmErase = false }) { Text("Cancel") } },
        )
    }
}

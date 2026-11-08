package com.blumlaut.filamenttagwriter.nfc

import android.nfc.Tag
import android.nfc.tech.MifareUltralight
import java.util.Locale

/** Inspect memory and erase ONLY the known filament/profile user-memory pages. */
object NfcTagTools {
    fun readMemory(tag: Tag): String {
        val chip = MifareUltralight.get(tag)
            ?: throw IllegalArgumentException("Tag is not NTAG213/215 compatible")
        chip.connect()
        try {
            val out = StringBuilder("NFC memory pages 0..39 (read-only)\n")
            for (page in 0..39) {
                val bytes = try {
                    chip.readPages(page)?.takeIf { it.size >= 4 }?.copyOfRange(0, 4)
                } catch (_: Exception) { null }
                out.append(String.format(Locale.US, "%02d (0x%02X): ", page, page))
                if (bytes == null) out.append("-- -- -- --")
                else out.append(bytes.joinToString(" ") { String.format(Locale.US, "%02X", it.toInt() and 255) })
                out.append('\n')
            }
            return out.toString()
        } finally {
            chip.close()
        }
    }

    /**
     * Erase ONLY pages 16..39 after separately confirmed UI action.
     * Never write UID (0..3), NDEF (4..15), lock bits or config (40+).
     * Non-atomic: a failed write may have partially cleared the tag.
     */
    fun clearFilamentProfile(tag: Tag): String {
        val chip = MifareUltralight.get(tag)
            ?: throw IllegalArgumentException("Tag is not NTAG213/215 compatible")
        chip.connect()
        var changed = 0
        try {
            for (page in 16..39) {
                val bytes = chip.readPages(page)
                require(bytes != null && bytes.size >= 4) {
                    "Cannot read page " + page + "; nothing was erased"
                }
            }
            val blank = ByteArray(4)
            for (page in 16..39) {
                chip.writePage(page, blank)
                changed++
            }
            for (page in 16..39) {
                val bytes = chip.readPages(page)
                check(bytes != null && bytes.size >= 4 && bytes.copyOfRange(0, 4).all { it == 0.toByte() }) {
                    "Read-back verification failed on page " + page
                }
            }
            return "Erased filament pages 16..39. UID, NDEF, lock/config pages unchanged."
        } catch (e: Exception) {
            val prefix = if (changed == 0) "Nothing written. " else "PARTIAL ERASE (" + changed + " pages): "
            throw IllegalStateException(prefix + e.message, e)
        } finally {
            chip.close()
        }
    }
}

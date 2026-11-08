package com.blumlaut.filamenttagwriter.data.model

import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction

/**
 * Optional CC2X record in NTAG213/215 pages 25..39. Original ELEGOO
 * pages 16..24 are NEVER modified by this encoder.
 * V1 = brand/name and CRC16. V2 adds heated-bed range and CRC16.
 */
object TagExtraMetadata {
    const val FIRST_PAGE = 25
    const val LAST_PAGE = 39
    const val PAGE_SIZE = 4
    const val CAPACITY = (LAST_PAGE - FIRST_PAGE + 1) * PAGE_SIZE
    const val MAX_TEXT_BYTES = CAPACITY - 14
    private const val LEGACY_MAX_TEXT_BYTES = CAPACITY - 10

    data class Details(
        val brand: String,
        val name: String,
        val bedMinTemp: Int? = null,
        val bedMaxTemp: Int? = null,
    )

    private val magic = byteArrayOf(0x43, 0x43, 0x32, 0x58)

    fun encode(brand: String, name: String, bedMinTemp: Int = 0, bedMaxTemp: Int = 0): ByteArray {
        val b = brand.trim().toByteArray(Charsets.UTF_8)
        val n = name.trim().toByteArray(Charsets.UTF_8)
        require(n.isNotEmpty()) { "Filament name cannot be empty" }
        require(b.size + n.size <= MAX_TEXT_BYTES) { "Brand + name exceed $MAX_TEXT_BYTES UTF-8 bytes" }
        require(bedMinTemp in 0..150 && bedMaxTemp in 0..150 && bedMinTemp <= bedMaxTemp) {
            "Bed temperature must satisfy 0 <= min <= max <= 150 °C"
        }
        val data = ByteArray(CAPACITY)
        magic.copyInto(data)
        data[4] = 2
        data[5] = b.size.toByte()
        data[6] = n.size.toByte()
        data[7] = 0
        b.copyInto(data, 8)
        n.copyInto(data, 8 + b.size)
        val at = 8 + b.size + n.size
        put16(data, at, bedMinTemp)
        put16(data, at + 2, bedMaxTemp)
        put16(data, at + 4, crc16(data, at + 4))
        return data
    }

    /** Null indicates no CC2X marker; malformed CC2X raises an exception. */
    fun decode(data: ByteArray): Details? {
        require(data.size == CAPACITY)
        if (!data.copyOfRange(0, 4).contentEquals(magic)) return null
        val version = data[4].toInt() and 255
        require(version == 1 || version == 2) { "Unsupported CC2X version" }
        require(data[7] == 0.toByte()) { "Unknown CC2X flags" }
        val bl = data[5].toInt() and 255
        val nl = data[6].toInt() and 255
        val max = if (version == 1) LEGACY_MAX_TEXT_BYTES else MAX_TEXT_BYTES
        require(nl > 0 && bl + nl <= max) { "Invalid CC2X text lengths" }
        val end = 8 + bl + nl
        val bedMin = if (version == 2) get16(data, end) else null
        val bedMax = if (version == 2) get16(data, end + 2) else null
        if (version == 2) require(bedMin!! in 0..150 && bedMax!! in 0..150 && bedMin <= bedMax) {
            "Invalid heated-bed temperature range"
        }
        val crcPos = end + if (version == 2) 4 else 0
        require(get16(data, crcPos) == crc16(data, crcPos)) { "CC2X CRC mismatch" }
        require((crcPos + 2 until CAPACITY).all { data[it] == 0.toByte() }) { "Invalid CC2X trailing data" }
        return Details(utf8(data, 8, bl), utf8(data, 8 + bl, nl), bedMin, bedMax)
    }

    fun canOverwrite(data: ByteArray): Boolean {
        if (data.size != CAPACITY) return false
        if (data.all { it == 0.toByte() }) return true
        return try { decode(data) != null } catch (_: Exception) { false }
    }

    private fun get16(data: ByteArray, at: Int): Int =
        ((data[at].toInt() and 255) shl 8) or (data[at + 1].toInt() and 255)

    private fun put16(data: ByteArray, at: Int, value: Int) {
        data[at] = (value shr 8).toByte()
        data[at + 1] = value.toByte()
    }

    private fun utf8(data: ByteArray, at: Int, size: Int): String {
        val dec = Charsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
        return dec.decode(ByteBuffer.wrap(data, at, size)).toString()
    }

    private fun crc16(data: ByteArray, length: Int): Int {
        var crc = 0xffff
        for (i in 0 until length) {
            crc = crc xor ((data[i].toInt() and 255) shl 8)
            repeat(8) {
                crc = if ((crc and 0x8000) != 0) ((crc shl 1) xor 0x1021) and 0xffff
                else (crc shl 1) and 0xffff
            }
        }
        return crc
    }
}

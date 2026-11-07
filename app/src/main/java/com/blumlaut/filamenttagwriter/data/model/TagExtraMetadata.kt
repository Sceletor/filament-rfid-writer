package com.blumlaut.filamenttagwriter.data.model

import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction

/**
 * Optional CC2X v1 metadata in NTAG213/215 user memory, pages 25 through 39.
 * Original ELEGOO payload at pages 16..24 remains unchanged.
 * The stock CANVAS firmware is NOT known to understand or accept this extension.
 * Never overwrite unknown nonzero data in the optional region.
 */
object TagExtraMetadata {
    const val FIRST_PAGE = 25
    const val LAST_PAGE = 39
    const val PAGE_SIZE = 4
    const val CAPACITY = (LAST_PAGE - FIRST_PAGE + 1) * PAGE_SIZE
    const val MAX_TEXT_BYTES = CAPACITY - 10

    data class Details(val brand: String, val name: String)

    private val magic = byteArrayOf(0x43, 0x43, 0x32, 0x58) // CC2X
    private const val VERSION = 1

    fun encode(brand: String, name: String): ByteArray {
        val brandBytes = brand.trim().toByteArray(Charsets.UTF_8)
        val nameBytes = name.trim().toByteArray(Charsets.UTF_8)
        require(nameBytes.isNotEmpty()) { "Filament name must not be empty" }
        require(brandBytes.size + nameBytes.size <= MAX_TEXT_BYTES) {
            "Brand and name exceed $MAX_TEXT_BYTES UTF-8 bytes; shorten the text"
        }
        val data = ByteArray(CAPACITY)
        magic.copyInto(data)
        data[4] = VERSION.toByte()
        data[5] = brandBytes.size.toByte()
        data[6] = nameBytes.size.toByte()
        data[7] = 0
        brandBytes.copyInto(data, 8)
        nameBytes.copyInto(data, 8 + brandBytes.size)
        val end = 8 + brandBytes.size + nameBytes.size
        val crc = crc16(data, end)
        data[end] = (crc shr 8).toByte()
        data[end + 1] = crc.toByte()
        return data
    }

    /** Return null for legacy tags; throw on corrupt CC2X data. */
    fun decode(data: ByteArray): Details? {
        require(data.size == CAPACITY) { "Expected $CAPACITY bytes" }
        if (!data.copyOfRange(0, 4).contentEquals(magic)) return null
        require((data[4].toInt() and 0xff) == VERSION) { "Unsupported CC2X version" }
        require(data[7] == 0.toByte()) { "Unsupported CC2X flags" }
        val brandLength = data[5].toInt() and 0xff
        val nameLength = data[6].toInt() and 0xff
        require(nameLength > 0 && brandLength + nameLength <= MAX_TEXT_BYTES) { "Invalid CC2X lengths" }
        val end = 8 + brandLength + nameLength
        val expected = ((data[end].toInt() and 0xff) shl 8) or (data[end + 1].toInt() and 0xff)
        require(expected == crc16(data, end)) { "CC2X checksum mismatch" }
        require((end + 2 until CAPACITY).all { data[it] == 0.toByte() }) { "CC2X trailing bytes are not empty" }
        return Details(utf8(data, 8, brandLength), utf8(data, 8 + brandLength, nameLength))
    }

    /** Only blank bytes or a valid existing CC2X record may be replaced. */
    fun canOverwrite(data: ByteArray): Boolean {
        if (data.size != CAPACITY) return false
        if (data.all { it == 0.toByte() }) return true
        return try { decode(data) != null } catch (_: IllegalArgumentException) { false }
    }

    private fun utf8(data: ByteArray, offset: Int, length: Int): String {
        val decoder = Charsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
        return decoder.decode(ByteBuffer.wrap(data, offset, length)).toString()
    }

    private fun crc16(data: ByteArray, size: Int): Int {
        var crc = 0xffff
        for (i in 0 until size) {
            crc = crc xor ((data[i].toInt() and 0xff) shl 8)
            repeat(8) {
                crc = if ((crc and 0x8000) != 0)
                    ((crc shl 1) xor 0x1021) and 0xffff
                else
                    (crc shl 1) and 0xffff
            }
        }
        return crc
    }
}

import com.blumlaut.filamenttagwriter.data.model.TagExtraMetadata

fun main() {
    val examples = listOf(
        Triple("SUNLU", "PLA+ 2.0", 55 to 65),
        Triple("JAYO", "PLA Matte", 40 to 60),
        Triple("Próba", "Szary PLA+", 0 to 0),
        Triple("", "PLA-CF", 60 to 90)
    )
    for ((brand, name, temps) in examples) {
        val data = TagExtraMetadata.encode(brand, name, temps.first, temps.second)
        check(data.size == 60)
        check(TagExtraMetadata.decode(data) ==
            TagExtraMetadata.Details(brand, name, temps.first, temps.second))
        check(TagExtraMetadata.canOverwrite(data))
        data[10] = (data[10].toInt() xor 1).toByte()
        check(!TagExtraMetadata.canOverwrite(data))
        check(runCatching { TagExtraMetadata.decode(data) }.isFailure)
    }
    check(TagExtraMetadata.canOverwrite(ByteArray(60)))
    check(!TagExtraMetadata.canOverwrite(ByteArray(60) { 7 }))
    check(TagExtraMetadata.decode(ByteArray(60)) == null)
    check(runCatching { TagExtraMetadata.encode("SUNLU", "X".repeat(47)) }.isFailure)
    check(runCatching { TagExtraMetadata.encode("SUNLU", "PLA", 90, 30) }.isFailure)
    println("CC2X v2 codec smoke tests passed")
}

// Standalone JVM smoke test: kotlinc TagExtraMetadata.kt CC2XCodecSmokeTest.kt -include-runtime -d cc2x.jar
// then: java -jar cc2x.jar
import com.blumlaut.filamenttagwriter.data.model.TagExtraMetadata

fun main() {
    val samples = listOf(
        "SUNLU" to "PLA+ 2.0",
        "JAYO" to "PLA Matte Slate Grey",
        "Próba" to "Szary PLA+",
        "" to "PLA-CF"
    )
    for ((brand, name) in samples) {
        val bytes = TagExtraMetadata.encode(brand, name)
        check(bytes.size == 60)
        check(TagExtraMetadata.decode(bytes) == TagExtraMetadata.Details(brand, name))
        check(TagExtraMetadata.canOverwrite(bytes))
        bytes[11] = (bytes[11].toInt() xor 1).toByte()
        check(!TagExtraMetadata.canOverwrite(bytes))
        check(runCatching { TagExtraMetadata.decode(bytes) }.isFailure)
    }
    check(TagExtraMetadata.canOverwrite(ByteArray(60)))
    check(!TagExtraMetadata.canOverwrite(ByteArray(60) { 7 }))
    check(TagExtraMetadata.decode(ByteArray(60)) == null)
    check(runCatching { TagExtraMetadata.encode("SUNLU", "X".repeat(51)) }.isFailure)
    println("CC2X codec smoke tests passed")
}

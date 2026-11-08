package com.blumlaut.filamenttagwriter.data.model

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Portable JSON profile backup. Strict schema marker, bounded import, new UUIDs
 * to avoid overwriting existing catalog entries.
 */
object FilamentCatalogBackup {
    fun encode(filaments: List<Filament>): String {
        val array = JSONArray()
        for (f in filaments) {
            val j = JSONObject()
            j.put("name", f.name)
            j.put("brand", f.brand)
            j.put("manufacturerCode", f.manufacturerCode)
            j.put("material", f.material)
            j.put("subtypeCode", f.subtypeCode.toInt())
            j.put("subtype", f.subtype)
            j.put("colorRgb", f.colorRgb)
            j.put("colorModifier", f.colorModifier.toInt())
            j.put("minTemp", f.minTemp.toInt())
            j.put("maxTemp", f.maxTemp.toInt())
            j.put("bedMinTemp", f.bedMinTemp.toInt())
            j.put("bedMaxTemp", f.bedMaxTemp.toInt())
            j.put("diameter", f.diameter.toDouble())
            j.put("weight", f.weight)
            j.put("productionDateRaw", f.productionDateRaw.toInt())
            array.put(j)
        }
        return JSONObject().put("format", "cc2x-filaments")
            .put("schema", 1).put("filaments", array).toString(2)
    }

    fun decode(source: String): List<Filament> {
        require(source.length <= 2_000_000) { "Backup file is too large (2 MB limit)" }
        val root = JSONObject(source)
        require(root.optString("format") == "cc2x-filaments" && root.optInt("schema") == 1) {
            "Not a recognized CC2X catalog backup"
        }
        val entries = root.getJSONArray("filaments")
        require(entries.length() <= 5000) { "Too many profiles" }
        val out = ArrayList<Filament>(entries.length())
        for (i in 0 until entries.length()) {
            val j = entries.getJSONObject(i)
            val name = j.getString("name")
            require(name.isNotBlank() && name.length <= 200) { "Invalid name at entry " + i }
            val minTemp = j.getInt("minTemp")
            val maxTemp = j.getInt("maxTemp")
            val bmin = j.optInt("bedMinTemp", 0)
            val bmax = j.optInt("bedMaxTemp", 0)
            val weight = j.getInt("weight")
            val diameter = j.getDouble("diameter")
            require(minTemp in 0..400 && maxTemp in 0..400 && minTemp <= maxTemp)
            require(bmin in 0..150 && bmax in 0..150 && bmin <= bmax)
            require(weight in 0..65535 && diameter >= 0.1 && diameter <= 10)
            val color = j.getInt("colorRgb")
            require(color in 0..0xffffff)
            out.add(
                Filament(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    brand = j.optString("brand", ""),
                    manufacturerCode = j.optInt("manufacturerCode", 0xEEEEEEEE.toInt()),
                    material = j.getString("material"),
                    subtypeCode = j.optInt("subtypeCode", 0).toShort(),
                    subtype = j.getString("subtype"),
                    colorRgb = color,
                    color = Epc256Encoder.rgbToHex(color),
                    colorModifier = j.optInt("colorModifier", 0).toByte(),
                    minTemp = minTemp.toShort(),
                    maxTemp = maxTemp.toShort(),
                    bedMinTemp = bmin.toShort(),
                    bedMaxTemp = bmax.toShort(),
                    diameter = diameter.toFloat(),
                    weight = weight,
                    productionDateRaw = j.optInt("productionDateRaw", 0).toShort(),
                )
            )
        }
        return out
    }
}

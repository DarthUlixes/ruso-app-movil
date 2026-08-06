package com.example.rusoit.data.model

import android.util.Log
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

/**
 * Parseo tolerante de respuestas group-by-date del backend Nest/Prisma.
 */
object FolioStatsParser {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    fun parseTypeServiceRows(raw: JsonElement?): List<TypeServiceCount>? {
        if (raw == null) return null
        return try {
            extractArray(raw).map { obj ->
                TypeServiceCount(
                    id_type_service = readInt(obj, "id_type_service", "id_type", "id"),
                    id_type = readInt(obj, "id_type"),
                    id = readInt(obj, "id"),
                    countRaw = obj["_count"],
                    count = readInt(obj, "count", "total", "quantity"),
                    total = readInt(obj, "total"),
                    quantity = readInt(obj, "quantity")
                )
            }
        } catch (e: Exception) {
            Log.e("FolioStatsParser", "type rows failed: $raw", e)
            null
        }
    }

    fun parseStatusRows(raw: JsonElement?): List<StatusCount>? {
        if (raw == null) return null
        return try {
            extractArray(raw).map { obj ->
                StatusCount(
                    status = readString(obj, "status", "name"),
                    name = readString(obj, "name"),
                    countRaw = obj["_count"],
                    count = readInt(obj, "count", "total", "quantity"),
                    total = readInt(obj, "total"),
                    quantity = readInt(obj, "quantity")
                )
            }
        } catch (e: Exception) {
            Log.e("FolioStatsParser", "status rows failed: $raw", e)
            null
        }
    }

    fun parseCologneRows(raw: JsonElement?): List<CologneCount>? {
        if (raw == null) return null
        return try {
            extractArray(raw).map { obj ->
                CologneCount(
                    id_cologne = readInt(obj, "id_cologne", "id_colgne", "id"),
                    id = readInt(obj, "id"),
                    countRaw = obj["_count"],
                    count = readInt(obj, "count", "total", "quantity"),
                    total = readInt(obj, "total"),
                    quantity = readInt(obj, "quantity")
                )
            }
        } catch (e: Exception) {
            Log.e("FolioStatsParser", "cologne rows failed: $raw", e)
            null
        }
    }

    private fun extractArray(raw: JsonElement): List<JsonObject> {
        val array: JsonArray = when (raw) {
            is JsonArray -> raw
            is JsonObject -> {
                val nested = raw["data"] ?: raw["result"] ?: raw["items"] ?: raw["rows"]
                when (nested) {
                    is JsonArray -> nested
                    else -> {
                        // A veces llega un solo objeto
                        return listOf(raw)
                    }
                }
            }
            else -> JsonArray(emptyList())
        }
        return array.mapNotNull { el ->
            when (el) {
                is JsonObject -> el
                else -> null
            }
        }
    }

    private fun readString(obj: JsonObject, vararg keys: String): String? {
        for (key in keys) {
            val value = obj[key] ?: continue
            when (value) {
                is JsonNull -> continue
                is JsonPrimitive -> {
                    val text = value.contentOrNull?.trim()
                    if (!text.isNullOrEmpty()) return text
                }
                else -> continue
            }
        }
        return null
    }

    private fun readInt(obj: JsonObject, vararg keys: String): Int? {
        for (key in keys) {
            val value = obj[key] ?: continue
            val parsed = when (value) {
                is JsonNull -> null
                is JsonPrimitive -> value.intOrNull
                    ?: value.longOrNull?.toInt()
                    ?: value.contentOrNull?.toDoubleOrNull()?.toInt()
                is JsonObject -> FlexibleCountSerializer.parseCount(value)
                else -> null
            }
            if (parsed != null) return parsed
        }
        return null
    }
}

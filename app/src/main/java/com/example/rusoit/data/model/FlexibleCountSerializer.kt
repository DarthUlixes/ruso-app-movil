package com.example.rusoit.data.model

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

/**
 * El backend Nest/Prisma a veces envía `_count` como número
 * y a veces como objeto `{ id | status | _all }`.
 */
object FlexibleCountSerializer {
    fun parseCount(element: JsonElement?): Int {
        return when (element) {
            null, JsonNull -> 0
            is JsonPrimitive -> element.intOrNull
                ?: element.contentOrNull?.toDoubleOrNull()?.toInt()
                ?: 0
            is JsonObject -> {
                listOf("_all", "status", "id", "count", "total", "quantity")
                    .firstNotNullOfOrNull { key ->
                        element[key]?.jsonPrimitive?.intOrNull
                            ?: element[key]?.jsonPrimitive?.contentOrNull?.toDoubleOrNull()?.toInt()
                    } ?: 0
            }
            else -> 0
        }
    }
}

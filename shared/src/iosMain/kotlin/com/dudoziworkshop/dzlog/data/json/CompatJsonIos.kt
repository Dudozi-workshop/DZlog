package com.dudoziworkshop.dzlog.data.json

import kotlinx.serialization.json.*

private fun wrap(value: JsonElement?): Any? = when (value) {
    null -> null
    JsonNull -> CompatJsonObject.NULL
    is JsonObject -> CompatJsonObject(value.mapValues { wrap(it.value) }.toMutableMap())
    is JsonArray -> CompatJsonArray(value.map { wrap(it) }.toMutableList())
    is JsonPrimitive -> if (value.isString) value.content else {
        value.booleanOrNull ?: value.content.toIntOrNull() ?: value.content.toLongOrNull()
            ?: value.content.toDoubleOrNull() ?: error("Invalid JSON primitive")
    }
}
private fun element(value: Any?): JsonElement = when (value) {
    null, CompatJsonObject.NULL -> JsonNull
    is CompatJsonObject -> JsonObject(value.values.mapValues { element(it.value) })
    is CompatJsonArray -> JsonArray(value.values.map { element(it) })
    is String -> JsonPrimitive(value)
    is Boolean -> JsonPrimitive(value)
    is Number -> {
        require(value.toDouble().isFinite()) { "Non-finite JSON number" }
        JsonPrimitive(value)
    }
    else -> error("Unsupported JSON value")
}
private fun text(value: Any?, fallback: String): String = when (value) {
    null, CompatJsonObject.NULL -> fallback
    else -> value.toString()
}
private fun number(value: Any?): Number? = when (value) {
    is Number -> value
    is String -> value.toDoubleOrNull()
    else -> null
}
private fun boolean(value: Any?, fallback: Boolean): Boolean = when (value) {
    is Boolean -> value
    is String -> when {
        value.equals("true", true) -> true
        value.equals("false", true) -> false
        else -> fallback
    }
    else -> fallback
}

internal actual class CompatJsonObject internal constructor(internal val values: MutableMap<String, Any?>) {
    actual constructor() : this(linkedMapOf())
    actual constructor(json: String) : this(Json.parseToJsonElement(json).jsonObject.mapValues { wrap(it.value) }.toMutableMap())
    actual companion object { actual val NULL: Any = JsonNull }
    actual fun has(key: String): Boolean = values.containsKey(key)
    actual fun opt(key: String): Any? = values[key]
    actual fun getInt(key: String): Int = requireNotNull(number(opt(key))).toInt()
    actual fun getString(key: String): String = requireNotNull(opt(key) as? String)
    actual fun getJSONObject(key: String): CompatJsonObject = requireNotNull(optJSONObject(key))
    actual fun getJSONArray(key: String): CompatJsonArray = requireNotNull(optJSONArray(key))
    actual fun optJSONObject(key: String): CompatJsonObject? = opt(key) as? CompatJsonObject
    actual fun optJSONArray(key: String): CompatJsonArray? = opt(key) as? CompatJsonArray
    actual fun optString(key: String, fallback: String): String = text(opt(key), fallback)
    actual fun optInt(key: String, fallback: Int): Int = number(opt(key))?.toInt() ?: fallback
    actual fun optLong(key: String, fallback: Long): Long = number(opt(key))?.toLong() ?: fallback
    actual fun optBoolean(key: String, fallback: Boolean): Boolean = boolean(opt(key), fallback)
    actual fun put(key: String, value: Any?): CompatJsonObject = apply {
        if (value == null) values.remove(key) else values[key] = value
    }
    actual override fun toString(): String = JsonObject(values.mapValues { element(it.value) }).toString()
}

internal actual class CompatJsonArray internal constructor(internal val values: MutableList<Any?>) {
    actual constructor() : this(mutableListOf())
    actual constructor(json: String) : this(Json.parseToJsonElement(json).jsonArray.map { wrap(it) }.toMutableList())
    actual fun length(): Int = values.size
    actual fun opt(index: Int): Any? = values.getOrNull(index)
    actual fun optJSONObject(index: Int): CompatJsonObject? = opt(index) as? CompatJsonObject
    actual fun optString(index: Int, fallback: String): String = text(opt(index), fallback)
    actual fun optDouble(index: Int, fallback: Double): Double = number(opt(index))?.toDouble() ?: fallback
    actual fun getJSONObject(index: Int): CompatJsonObject = requireNotNull(optJSONObject(index))
    actual fun getString(index: Int): String = requireNotNull(opt(index) as? String)
    actual fun put(value: Any?): CompatJsonArray = apply { values.add(value ?: CompatJsonObject.NULL) }
    actual override fun toString(): String = JsonArray(values.map { element(it) }).toString()
}

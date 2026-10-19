package com.dudoziworkshop.dzlog.data.json

import org.json.JSONArray
import org.json.JSONObject

private fun wrap(value: Any?): Any? = when (value) {
    is JSONObject -> CompatJsonObject(value)
    is JSONArray -> CompatJsonArray(value)
    else -> value
}
private fun unwrap(value: Any?): Any? = when (value) {
    is CompatJsonObject -> value.nativeValue
    is CompatJsonArray -> value.nativeValue
    else -> value
}

internal actual class CompatJsonObject internal constructor(internal val nativeValue: JSONObject) {
    actual constructor() : this(JSONObject())
    actual constructor(json: String) : this(JSONObject(json))
    actual companion object { actual val NULL: Any = JSONObject.NULL }
    actual fun has(key: String): Boolean = nativeValue.has(key)
    actual fun opt(key: String): Any? = wrap(nativeValue.opt(key))
    actual fun getInt(key: String): Int = nativeValue.getInt(key)
    actual fun getString(key: String): String = nativeValue.getString(key)
    actual fun getJSONObject(key: String): CompatJsonObject = CompatJsonObject(nativeValue.getJSONObject(key))
    actual fun getJSONArray(key: String): CompatJsonArray = CompatJsonArray(nativeValue.getJSONArray(key))
    actual fun optJSONObject(key: String): CompatJsonObject? = nativeValue.optJSONObject(key)?.let(::CompatJsonObject)
    actual fun optJSONArray(key: String): CompatJsonArray? = nativeValue.optJSONArray(key)?.let(::CompatJsonArray)
    actual fun optString(key: String, fallback: String): String = nativeValue.optString(key, fallback)
    actual fun optInt(key: String, fallback: Int): Int = nativeValue.optInt(key, fallback)
    actual fun optLong(key: String, fallback: Long): Long = nativeValue.optLong(key, fallback)
    actual fun optBoolean(key: String, fallback: Boolean): Boolean = nativeValue.optBoolean(key, fallback)
    actual fun put(key: String, value: Any?): CompatJsonObject = apply { nativeValue.put(key, unwrap(value)) }
    actual override fun toString(): String = nativeValue.toString()
}

internal actual class CompatJsonArray internal constructor(internal val nativeValue: JSONArray) {
    actual constructor() : this(JSONArray())
    actual constructor(json: String) : this(JSONArray(json))
    actual fun length(): Int = nativeValue.length()
    actual fun opt(index: Int): Any? = wrap(nativeValue.opt(index))
    actual fun optJSONObject(index: Int): CompatJsonObject? = nativeValue.optJSONObject(index)?.let(::CompatJsonObject)
    actual fun optString(index: Int, fallback: String): String = nativeValue.optString(index, fallback)
    actual fun optDouble(index: Int, fallback: Double): Double = nativeValue.optDouble(index, fallback)
    actual fun getJSONObject(index: Int): CompatJsonObject = CompatJsonObject(nativeValue.getJSONObject(index))
    actual fun getString(index: Int): String = nativeValue.getString(index)
    actual fun put(value: Any?): CompatJsonArray = apply { nativeValue.put(unwrap(value)) }
    actual override fun toString(): String = nativeValue.toString()
}

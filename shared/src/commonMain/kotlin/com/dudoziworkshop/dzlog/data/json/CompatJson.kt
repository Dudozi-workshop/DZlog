package com.dudoziworkshop.dzlog.data.json

/** Small JSON boundary; JVM delegates to the existing Android org.json API. */
internal expect class CompatJsonObject {
    constructor()
    constructor(json: String)
    companion object { val NULL: Any }
    fun has(key: String): Boolean
    fun opt(key: String): Any?
    fun getInt(key: String): Int
    fun getString(key: String): String
    fun getJSONObject(key: String): CompatJsonObject
    fun getJSONArray(key: String): CompatJsonArray
    fun optJSONObject(key: String): CompatJsonObject?
    fun optJSONArray(key: String): CompatJsonArray?
    fun optString(key: String, fallback: String = ""): String
    fun optInt(key: String, fallback: Int = 0): Int
    fun optLong(key: String, fallback: Long = 0): Long
    fun optBoolean(key: String, fallback: Boolean = false): Boolean
    fun put(key: String, value: Any?): CompatJsonObject
    override fun toString(): String
}

internal expect class CompatJsonArray {
    constructor()
    constructor(json: String)
    fun length(): Int
    fun opt(index: Int): Any?
    fun optJSONObject(index: Int): CompatJsonObject?
    fun optString(index: Int, fallback: String = ""): String
    fun optDouble(index: Int, fallback: Double = Double.NaN): Double
    fun getJSONObject(index: Int): CompatJsonObject
    fun getString(index: Int): String
    fun put(value: Any?): CompatJsonArray
    override fun toString(): String
}

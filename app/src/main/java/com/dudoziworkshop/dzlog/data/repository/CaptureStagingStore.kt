package com.dudoziworkshop.dzlog.data.repository

import android.content.Context
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Independent staging foundation. Not activated by the camera pipeline until
 * capture snapshots and recovery workers are integrated and tested.
 */
internal class CaptureStagingStore(context: Context) {
    private val root = File(context.filesDir, "capture-staging-v1").also {
        check(it.isDirectory || it.mkdirs()) { "Cannot create staging directory" }
    }

    internal data class StagedCapture(
        val id: String,
        val counter: Int,
        val relativePathKey: String,
        val streamPrefix: String,
        val displayName: String,
        val createdAtMillis: Long,
        val originalFile: File,
    )

    /** Persist bytes before metadata, so a journal entry never points at an incomplete copy. */
    @Synchronized
    fun stage(
        id: String,
        sourceJpeg: File,
        counter: Int,
        relativePathKey: String,
        streamPrefix: String,
        displayName: String,
        createdAtMillis: Long,
    ): StagedCapture {
        require(Regex("[0-9a-fA-F-]{36}").matches(id)) { "Invalid capture ID" }
        require(counter > 0 && sourceJpeg.isFile) { "Invalid source image or counter" }
        require(!File(root, "$id.json").exists()) { "Capture already staged" }
        val tempImage = File(root, "$id.jpg.part")
        val stagedImage = File(root, "$id.jpg")
        val tempJournal = File(root, "$id.json.part")
        val journal = File(root, "$id.json")
        require(!stagedImage.exists() && !tempImage.exists()) { "Capture already exists" }
        try {
            sourceJpeg.inputStream().use { input ->
                FileOutputStream(tempImage).use { out ->
                    input.copyTo(out)
                    out.fd.sync()
                }
            }
            if (!tempImage.renameTo(stagedImage)) throw IOException("Cannot promote image")
            val payload = JSONObject()
                .put("version", 1)
                .put("id", id)
                .put("counter", counter)
                .put("relativePathKey", relativePathKey)
                .put("streamPrefix", streamPrefix)
                .put("displayName", displayName)
                .put("createdAtMillis", createdAtMillis)
            FileOutputStream(tempJournal).use { out ->
                out.write(payload.toString().toByteArray(Charsets.UTF_8))
                out.fd.sync()
            }
            if (!tempJournal.renameTo(journal)) throw IOException("Cannot commit journal")
            return StagedCapture(id, counter, relativePathKey, streamPrefix, displayName, createdAtMillis, stagedImage)
        } catch (e: Exception) {
            tempImage.delete()
            tempJournal.delete()
            // Preserve promoted JPEG on failure, for inspection/recovery.
            throw e
        }
    }

    /** Never delete malformed or orphaned entries during recovery. */
    @Synchronized
    fun recoverable(): List<StagedCapture> =
        (root.listFiles() ?: emptyArray())
            .filter { it.isFile && it.name.endsWith(".json") }
            .mapNotNull { journal ->
                runCatching {
                    val doc = JSONObject(journal.readText(Charsets.UTF_8))
                    check(doc.getInt("version") == 1)
                    val id = doc.getString("id")
                    check(journal.name == "$id.json")
                    val image = File(root, "$id.jpg")
                    check(image.isFile && image.length() > 0L)
                    StagedCapture(
                        id = id,
                        counter = doc.getInt("counter").also { check(it > 0) },
                        relativePathKey = doc.getString("relativePathKey"),
                        streamPrefix = doc.getString("streamPrefix"),
                        displayName = doc.getString("displayName"),
                        createdAtMillis = doc.getLong("createdAtMillis"),
                        originalFile = image,
                    )
                }.getOrNull()
            }
            .sortedWith(compareBy<StagedCapture> { it.createdAtMillis }.thenBy { it.id })
}

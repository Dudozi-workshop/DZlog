package com.dudoziworkshop.dzlog.data.repository

import android.content.Context
import android.net.Uri
import android.util.AtomicFile
import com.dudoziworkshop.dzlog.feature.capture.policy.CaptureReservation
import com.dudoziworkshop.dzlog.feature.capture.policy.CaptureReservationPolicy
import com.dudoziworkshop.dzlog.feature.capture.policy.CaptureReservationScope
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest
import java.util.UUID

/**
 * Persistent foundation for capture queue V2; not wired to the live camera yet.
 * All mutations are serialized across store instances in the app process.
 */
internal class CapturePendingStore(context: Context) {
    private val root = File(context.filesDir, "capture-pending-v2").also {
        check(it.isDirectory || it.mkdirs()) { "Cannot create capture pending directory" }
    }

    internal enum class Status { RESERVED, READY, PROCESSING, SAVED_UNCOMMITTED, COMMITTED, NEEDS_REVIEW }

    internal data class PendingCapture(
        val reservation: CaptureReservation,
        val status: Status,
        val snapshot: CaptureJobSnapshot,
        val imageFile: File,
        val outputUris: List<String>,
    )

    internal data class RecoveryInventory(
        val ready: List<PendingCapture>,
        val needsReview: List<PendingCapture>,
        val orphanFiles: List<String>,
        val corruptJournals: List<String>,
        val completedAwaitingApply: List<PendingCapture>,
    )

    fun reserveNext(
        scope: CaptureReservationScope,
        seed: Int,
        snapshotForCounter: (Int) -> CaptureJobSnapshot,
    ): PendingCapture = synchronized(lock) {
        // A damaged journal blocks new reservations until reviewed: no accidental reuse.
        ensureNoInterruptedJournals()
        val existing = journalFiles().map { file -> readDocument(file) }
        val pending = existing.map { doc -> reservationFrom(doc) }
        val counter = CaptureReservationPolicy.nextCounter(scope, seed, pending)
        val reservation = CaptureReservationPolicy.create(scope, counter)
        val snapshot = snapshotForCounter(counter)
        require(snapshot.request.displayName.isNotBlank()) { "Capture filename must not be empty" }
        require(snapshot.request.relativePath.isNotBlank()) { "Capture destination must not be empty" }
        val doc = JSONObject()
            .put("version", 2)
            .put("id", reservation.id)
            .put("pathKey", scope.relativePathKey)
            .put("prefix", scope.streamPrefix)
            .put("counter", counter)
            .put("createdAtMillis", reservation.createdAtMillis)
            .put("status", Status.RESERVED.name)
            .put("snapshot", CaptureJobSnapshotCodec.encode(snapshot))
            .put("outputs", JSONArray())
        writeDocument(reservation.id, doc)
        parseDocument(doc)
    }

    /** Call after CameraX has finished writing the captured temporary JPEG. */
    fun stageJpeg(id: String, sourceJpeg: File): PendingCapture = synchronized(lock) {
        val doc = readDocument(journalFile(id))
        check(statusOf(doc) == Status.RESERVED) { "Capture is not awaiting JPEG" }
        require(sourceJpeg.isFile && sourceJpeg.length() > 0L) { "Missing JPEG" }
        val partial = File(root, "$id.jpg.part")
        val image = imageFile(id)
        check(!partial.exists() && !image.exists()) { "Staged image already present" }
        try {
            val digest = MessageDigest.getInstance("SHA-256")
            FileOutputStream(partial).use { out ->
                sourceJpeg.inputStream().use { input ->
                    val buffer = ByteArray(16 * 1024)
                    while (true) {
                        val size = input.read(buffer)
                        if (size < 0) break
                        out.write(buffer, 0, size)
                        digest.update(buffer, 0, size)
                    }
                }
                out.fd.sync()
            }
            if (!partial.renameTo(image)) throw IOException("Unable to promote staged JPEG")
            doc.put("imageBytes", image.length())
                .put("imageSha256", digest.digest().joinToString("") { "%02x".format(it) })
                .put("status", Status.READY.name)
            writeDocument(id, doc)
            parseDocument(doc)
        } catch (error: Exception) {
            partial.delete()
            // A promoted image stays on disk even if journal update failed.
            throw error
        }
    }

    fun markProcessing(id: String): PendingCapture = synchronized(lock) {
        ensureNoInterruptedJournals()
        val doc = readDocument(journalFile(id))
        check(statusOf(doc) == Status.READY) { "Capture is not ready" }
        val staged = parseDocument(doc)
        check(validImage(staged)) { "Staged JPEG failed integrity verification" }
        doc.put("status", Status.PROCESSING.name)
        writeDocument(id, doc)
        parseDocument(doc)
    }

    /** Write output URIs before advancing the counter/phrase progress. */
    fun markMediaSaved(id: String, savedUris: List<Uri>): PendingCapture = synchronized(lock) {
        val doc = readDocument(journalFile(id))
        check(statusOf(doc) == Status.PROCESSING) { "Unexpected capture state" }
        val mode = CaptureJobSnapshotCodec.decode(doc.getJSONObject("snapshot")).request.saveMode
        val expectedOutputs = if (mode == com.dudoziworkshop.dzlog.domain.model.SaveMode.BOTH) 2 else 1
        require(savedUris.size == expectedOutputs &&
            savedUris.none { it == Uri.EMPTY } &&
            savedUris.map(Uri::toString).distinct().size == expectedOutputs
        ) { "Incomplete or duplicated saved output set" }
        doc.put("outputs", JSONArray().apply { savedUris.forEach { put(it.toString()) } })
            .put("status", Status.SAVED_UNCOMMITTED.name)
        writeDocument(id, doc)
        parseDocument(doc)
    }

    fun markCommitted(id: String): PendingCapture =
        transition(id, Status.SAVED_UNCOMMITTED, Status.COMMITTED)

    /** Ambiguous processing failures are never automatically retried. */
    fun markNeedsReview(id: String): PendingCapture = synchronized(lock) {
        val doc = readDocument(journalFile(id))
        // A READY image can fail integrity verification before the worker
        // begins saving. Quarantine it instead of endlessly resubmitting it.
        check(statusOf(doc) == Status.READY || statusOf(doc) == Status.PROCESSING ||
            statusOf(doc) == Status.SAVED_UNCOMMITTED)
        doc.put("status", Status.NEEDS_REVIEW.name)
        writeDocument(id, doc)
        parseDocument(doc)
    }

    /**
     * Saved originals can be reclaimed after MediaStore and counter commit.
     * Retain the journal until the UI acknowledges the template/phrase patch.
     */
    fun pruneCommitted(id: String) = synchronized(lock) {
        val doc = readDocument(journalFile(id))
        check(statusOf(doc) == Status.COMMITTED) { "Cannot prune unfinished capture" }
        val image = imageFile(id)
        if (image.exists() && !image.delete()) throw IOException("Could not remove staged copy")
    }

    /** Acknowledges the template patch after successful application by the host. */
    fun acknowledgeTemplateApplied(id: String) = synchronized(lock) {
        val doc = readDocument(journalFile(id))
        check(statusOf(doc) == Status.COMMITTED) { "Cannot acknowledge unfinished capture" }
        val image = imageFile(id)
        if (image.exists() && !image.delete()) throw IOException("Could not remove staged copy")
        if (!journalFile(id).delete()) throw IOException("Unable to remove applied capture journal")
    }

    /** On restart: do not auto-retry PROCESSING; final output may already exist. */
    fun inspectRecovery(): RecoveryInventory = synchronized(lock) {
        val ready = mutableListOf<PendingCapture>()
        val review = mutableListOf<PendingCapture>()
        val unapplied = mutableListOf<PendingCapture>()
        val corrupt = interruptedJournalFiles().mapTo(mutableListOf()) { it.name }
        val known = mutableSetOf<String>()
        journalFiles().forEach { file ->
            known += file.name.removeSuffix(".json")
            runCatching {
                val record = parseDocument(readDocument(file))
                when {
                    record.status == Status.COMMITTED -> unapplied += record
                    record.status == Status.READY && validImage(record) -> ready += record
                    else -> review += record
                }
            }.onFailure { corrupt += file.name }
        }
        val orphans = root.listFiles().orEmpty().filter {
            (it.name.endsWith(".jpg") || it.name.endsWith(".part")) &&
                // CameraX writes <id>.camera.jpg.part before the staged copy exists.
                // It belongs to that journal while active, not an orphan.
                it.name.substringBefore(".camera.jpg.part")
                    .substringBefore(".jpg")
                    .substringBefore(".json") !in known
        }.map { it.name }
        RecoveryInventory(
            ready.sortedBy { it.reservation.createdAtMillis },
            review.sortedBy { it.reservation.createdAtMillis },
            orphans.sorted(), corrupt.sorted(),
            unapplied.sortedBy { it.reservation.createdAtMillis },
        )
    }

    private fun transition(id: String, from: Status, to: Status): PendingCapture =
        synchronized(lock) {
            val doc = readDocument(journalFile(id))
            check(statusOf(doc) == from) { "Unexpected capture state" }
            doc.put("status", to.name)
            writeDocument(id, doc)
            parseDocument(doc)
        }

    private fun validImage(record: PendingCapture): Boolean {
        val doc = readDocument(journalFile(record.reservation.id))
        if (!record.imageFile.isFile) return false
        if (record.imageFile.length() != doc.optLong("imageBytes", -1L)) return false
        val actualHash = MessageDigest.getInstance("SHA-256").let { digest ->
            record.imageFile.inputStream().use { stream ->
                val buffer = ByteArray(16 * 1024)
                while (true) {
                    val n = stream.read(buffer)
                    if (n < 0) break
                    digest.update(buffer, 0, n)
                }
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        }
        return actualHash == doc.optString("imageSha256")
    }

    private fun parseDocument(doc: JSONObject): PendingCapture {
        check(doc.getInt("version") == 2) { "Unknown journal version" }
        val reservation = reservationFrom(doc)
        val outputs = doc.getJSONArray("outputs")
        return PendingCapture(
            reservation = reservation,
            status = statusOf(doc),
            snapshot = CaptureJobSnapshotCodec.decode(doc.getJSONObject("snapshot")),
            imageFile = imageFile(reservation.id),
            outputUris = List(outputs.length()) { outputs.getString(it) },
        )
    }

    private fun reservationFrom(doc: JSONObject): CaptureReservation {
        check(doc.getInt("version") == 2)
        return CaptureReservation(
            id = doc.getString("id").also(::validateId),
            scope = CaptureReservationScope(doc.getString("pathKey"), doc.getString("prefix")),
            counter = doc.getInt("counter").also { check(it > 0) },
            createdAtMillis = doc.getLong("createdAtMillis"),
        )
    }

    private fun statusOf(doc: JSONObject): Status = Status.valueOf(doc.getString("status"))
    private fun journalFiles(): List<File> =
        root.listFiles().orEmpty().filter { it.isFile && it.name.endsWith(".json") }

    /**
     * AtomicFile may leave a .new (interrupted write) or .bak (legacy backup).
     * Never allocate another number while the last durable state is ambiguous.
     */
    private fun interruptedJournalFiles(): List<File> =
        root.listFiles().orEmpty().filter { file ->
            file.isFile && (
                file.name.endsWith(".json.new") ||
                    file.name.endsWith(".json.bak") ||
                    file.name.endsWith(".json.part")
                )
        }

    private fun ensureNoInterruptedJournals() {
        check(interruptedJournalFiles().isEmpty()) {
            "Interrupted capture journal requires recovery review"
        }
    }
    private fun imageFile(id: String): File = File(root, "${validateId(id)}.jpg")
    private fun journalFile(id: String): File = File(root, "${validateId(id)}.json")
    private fun validateId(id: String): String = UUID.fromString(id).toString().also {
        require(it == id) { "Invalid capture identifier" }
    }

    private fun readDocument(file: File): JSONObject =
        JSONObject(AtomicFile(file).openRead().bufferedReader(Charsets.UTF_8).use { it.readText() })
            .also { check(file.name == "${it.getString("id")}.json") }

    private fun writeDocument(id: String, doc: JSONObject) {
        val atomic = AtomicFile(journalFile(id))
        val stream = atomic.startWrite()
        try {
            stream.write(doc.toString().toByteArray(Charsets.UTF_8))
            atomic.finishWrite(stream)
        } catch (error: Exception) {
            atomic.failWrite(stream)
            throw error
        }
    }

    private companion object {
        val lock = Any()
    }
}

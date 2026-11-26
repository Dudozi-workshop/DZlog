@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.dudoziworkshop.dzlog.feature.capture.policy

import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import platform.CoreCrypto.CC_SHA256
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.posix.memcpy

/** Files are private staging inputs, not user-visible gallery assets. */
class IosDurableCaptureStagingStore(
    private val rootPath: String,
    private val fileManager: NSFileManager = NSFileManager.defaultManager,
) {
    init { require(rootPath.isNotBlank() && rootPath != "/") }

    fun stage(snapshot: DurableCaptureSnapshot, sourceJpeg: ByteArray) {
        require(snapshot.record.stage == DurableCaptureStage.READY) {
            "Only a complete snapshot may become ready."
        }
        validateCaptureId(snapshot.record.captureId)
        require(snapshot.sourceJpeg.fileName == SOURCE_FILE_NAME) {
            "Unexpected staged source name."
        }
        require(snapshot.sourceJpeg == sourceIdentity(snapshot.sourceJpeg.fileName, sourceJpeg)) {
            "Source JPEG identity changed before staging."
        }
        ensureDirectory(rootPath)
        val paths = paths(snapshot.record.captureId)
        check(!fileManager.fileExistsAtPath(paths.manifest)) { "Capture is already staged." }
        check(!fileManager.fileExistsAtPath(paths.sourceJpeg)) {
            "An incomplete source JPEG requires recovery before retry."
        }
        ensureDirectory(paths.directory)

        // Atomically publish the JPEG first. A crash here leaves no resumable manifest.
        writeAtomically(paths.sourceJpeg, sourceJpeg)
        try {
            verifySource(paths.sourceJpeg, snapshot.sourceJpeg)
            // Manifest is the final readiness marker and is itself atomically replaced.
            writeAtomically(paths.manifest,
                DurableCaptureSnapshotCodec.encode(snapshot).encodeToByteArray())
            check(load(snapshot.record.captureId) == snapshot) {
                "Staged capture failed its read-back check."
            }
        } catch (failure: Throwable) {
            runCatching { fileManager.removeItemAtPath(paths.manifest, error = null) }
            throw failure
        }
    }

    /** A missing manifest is an incomplete reservation, never a ready capture. */
    fun load(captureId: String): DurableCaptureSnapshot? {
        validateCaptureId(captureId)
        val paths = paths(captureId)
        if (!fileManager.fileExistsAtPath(paths.manifest)) return null
        val manifest = readBytes(paths.manifest).decodeToString()
        val snapshot = DurableCaptureSnapshotCodec.decode(manifest)
        check(snapshot.record.captureId == captureId) { "Capture directory identity mismatch." }
        check(snapshot.sourceJpeg.fileName == SOURCE_FILE_NAME) { "Unexpected staged source name." }
        verifySource(paths.sourceJpeg, snapshot.sourceJpeg)
        return snapshot
    }

    fun listSnapshots(): List<DurableCaptureSnapshot> {
        if (!fileManager.fileExistsAtPath(rootPath)) return emptyList()
        val names = fileManager.contentsOfDirectoryAtPath(rootPath, error = null)
            ?.mapNotNull { it as? String }
            .orEmpty()
        val snapshots = names.mapNotNull(::load).sortedBy { it.record.reservationOrder }
        check(DurableCaptureQueuePolicy.isWellFormed(snapshots.map(DurableCaptureSnapshot::record))) {
            "Staged capture queue is not well formed."
        }
        return snapshots
    }

    fun transition(captureId: String, next: DurableCaptureStage): DurableCaptureSnapshot {
        val current = checkNotNull(load(captureId)) { "Staged capture does not exist." }
        check(DurableCaptureQueuePolicy.canTransition(current.record.stage, next)) {
            "Unsafe capture stage transition."
        }
        val updated = current.copy(record = current.record.copy(stage = next))
        writeAtomically(paths(captureId).manifest,
            DurableCaptureSnapshotCodec.encode(updated).encodeToByteArray())
        return checkNotNull(load(captureId))
    }

    /** Recovery may remove a JPEG only when no readiness manifest was ever published. */
    fun discardIncomplete(captureId: String): Boolean {
        validateCaptureId(captureId)
        val paths = paths(captureId)
        check(!fileManager.fileExistsAtPath(paths.manifest)) {
            "A ready capture cannot be discarded as incomplete."
        }
        if (!fileManager.fileExistsAtPath(paths.directory)) return false
        check(fileManager.removeItemAtPath(paths.directory, error = null))
        return true
    }

    internal fun paths(captureId: String): Paths {
        validateCaptureId(captureId)
        val directory = "$rootPath/$captureId"
        return Paths(directory, "$directory/$SOURCE_FILE_NAME", "$directory/$MANIFEST_FILE_NAME")
    }

    private fun verifySource(path: String, expected: DurableCaptureSourceJpeg) {
        check(fileManager.fileExistsAtPath(path)) { "Staged source JPEG is missing." }
        val bytes = readBytes(path)
        check(sourceIdentity(expected.fileName, bytes) == expected) {
            "Staged source JPEG is damaged."
        }
    }

    private fun ensureDirectory(path: String) {
        if (!fileManager.fileExistsAtPath(path)) {
            check(fileManager.createDirectoryAtPath(path,
                withIntermediateDirectories = true, attributes = null, error = null))
        }
    }

    private fun writeAtomically(path: String, bytes: ByteArray) {
        require(bytes.isNotEmpty())
        check(bytes.toNSData().writeToFile(path, atomically = true))
    }

    private fun readBytes(path: String): ByteArray =
        checkNotNull(fileManager.contentsAtPath(path)) { "Staged file is unreadable." }.toByteArray()

    internal data class Paths(val directory: String, val sourceJpeg: String, val manifest: String)

    companion object {
        const val SOURCE_FILE_NAME = "source.jpg"
        private const val MANIFEST_FILE_NAME = "manifest.json"
        private val SAFE_CAPTURE_ID = Regex("[A-Za-z0-9._-]+")

        fun sourceIdentity(bytes: ByteArray): DurableCaptureSourceJpeg =
            sourceIdentity(SOURCE_FILE_NAME, bytes)

        private fun sourceIdentity(fileName: String, bytes: ByteArray): DurableCaptureSourceJpeg {
            require(bytes.isNotEmpty())
            val digest = ByteArray(32)
            bytes.usePinned { source -> digest.usePinned { target ->
                CC_SHA256(source.addressOf(0), bytes.size.convert(), target.addressOf(0).reinterpret())
            } }
            return DurableCaptureSourceJpeg(fileName, bytes.size.toLong(),
                digest.joinToString("") { (it.toInt() and 255).toString(16).padStart(2, '0') })
        }

        private fun validateCaptureId(value: String) {
            require(value.matches(SAFE_CAPTURE_ID) && value != "." && value != "..") {
                "Unsafe capture identifier."
            }
        }
    }
}

private fun ByteArray.toNSData(): NSData = usePinned { pinned ->
    NSData.create(bytes = pinned.addressOf(0), length = size.toULong())
}

private fun NSData.toByteArray(): ByteArray {
    val size = length.toInt()
    return ByteArray(size).also { output ->
        if (size > 0) output.usePinned { memcpy(it.addressOf(0), bytes, length) }
    }
}

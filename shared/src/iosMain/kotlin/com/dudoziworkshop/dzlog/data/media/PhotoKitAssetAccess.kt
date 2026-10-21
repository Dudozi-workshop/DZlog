@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package com.dudoziworkshop.dzlog.data.media

import com.dudoziworkshop.dzlog.domain.media.CaptureMediaRole
import com.dudoziworkshop.dzlog.domain.media.CaptureMediaSavePlan
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.usePinned
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import platform.Foundation.NSData
import platform.Foundation.NSMutableArray
import platform.Foundation.NSMutableData
import platform.Foundation.create
import platform.Photos.PHAccessLevelReadWrite
import platform.Photos.PHAsset
import platform.Photos.PHAssetChangeRequest
import platform.Photos.PHAssetCreationRequest
import platform.Photos.PHAssetResource
import platform.Photos.PHAssetResourceCreationOptions
import platform.Photos.PHAssetResourceManager
import platform.Photos.PHAssetResourceRequestOptions
import platform.Photos.PHAssetResourceTypePhoto
import platform.Photos.PHAuthorizationStatusAuthorized
import platform.Photos.PHAuthorizationStatusLimited
import platform.Photos.PHAuthorizationStatusNotDetermined
import platform.Photos.PHPhotoLibrary

internal data class PhotoKitCreatedBatch(
    val entries: List<IndexedCaptureMedia>,
    val metadataError: Throwable? = null,
)

/** Only this iOS adapter touches Photos; Android MediaStore remains independent. */
internal class PhotoKitAssetAccess {
    fun hasReadAccess(): Boolean = allowed(PHPhotoLibrary.authorizationStatusForAccessLevel(PHAccessLevelReadWrite))

    suspend fun ensureReadWriteAccess() {
        var status = PHPhotoLibrary.authorizationStatusForAccessLevel(PHAccessLevelReadWrite)
        if (status == PHAuthorizationStatusNotDetermined) {
            status = suspendCoroutine { continuation ->
                PHPhotoLibrary.requestAuthorizationForAccessLevel(PHAccessLevelReadWrite) {
                    continuation.resume(it)
                }
            }
        }
        check(allowed(status)) { "DZlog needs Photos read/write permission to save and restore photo assets." }
    }

    private fun allowed(status: platform.Photos.PHAuthorizationStatus): Boolean =
        status == PHAuthorizationStatusAuthorized || status == PHAuthorizationStatusLimited

    fun exists(assetId: String): Boolean = find(assetId) != null

    private fun find(assetId: String): PHAsset? {
        if (!hasReadAccess()) return null
        val fetched = PHAsset.fetchAssetsWithLocalIdentifiers(listOf(assetId), options = null)
        return fetched.firstObject as? PHAsset
    }

    /**
     * One PhotoKit change transaction creates all requested roles, or PhotoKit fails it.
     * The pending journal is flushed before the change block returns, allowing relaunch
     * reconciliation of the placeholders even if the process dies before completion.
     */
    suspend fun create(
        plan: CaptureMediaSavePlan,
        bytes: Map<CaptureMediaRole, ByteArray>,
        index: PhotoKitCaptureMediaIndex,
    ): PhotoKitCreatedBatch {
        val payloads = plan.targets.associate { it.role to bytes.getValue(it.role).toNSData() }
        return suspendCoroutine { continuation ->
            val entries = mutableListOf<IndexedCaptureMedia>()
            var metadataError: Throwable? = null
            PHPhotoLibrary.sharedPhotoLibrary().performChanges({
                try {
                    for (target in plan.targets) {
                        val request = PHAssetCreationRequest.creationRequestForAsset()
                        val options = PHAssetResourceCreationOptions()
                        options.originalFilename = target.displayName
                        request.addResourceWithType(PHAssetResourceTypePhoto, data = payloads.getValue(target.role), options = options)
                        val identifier = request.placeholderForCreatedAsset?.localIdentifier
                            ?: error("PhotoKit did not return an asset placeholder.")
                        entries += IndexedCaptureMedia(
                            plan.captureId,
                            com.dudoziworkshop.dzlog.domain.media.PersistedCaptureAsset(target, identifier),
                        )
                    }
                    index.stagePending(entries)
                } catch (exception: Throwable) {
                    metadataError = exception
                }
            }, completionHandler = { success, error ->
                if (!success) {
                    // An unsuccessful PhotoKit change block has no committed assets.
                    runCatching { index.clearPending() }
                    continuation.resumeWithException(
                        IllegalStateException("PhotoKit could not save capture: ${error?.localizedDescription}", metadataError),
                    )
                } else {
                    continuation.resume(PhotoKitCreatedBatch(entries.toList(), metadataError))
                }
            })
        }
    }

    suspend fun remove(assetIds: List<String>): Boolean {
        val assets = assetIds.mapNotNull(::find)
        if (assets.isEmpty()) return true
        return suspendCoroutine { continuation ->
            PHPhotoLibrary.sharedPhotoLibrary().performChanges({
                val nativeAssets = NSMutableArray()
                assets.forEach { nativeAssets.addObject(it) }
                PHAssetChangeRequest.deleteAssets(nativeAssets)
            }, completionHandler = { success, _ -> continuation.resume(success) })
        }
    }

    suspend fun read(assetId: String): ByteArray? {
        val asset = find(assetId) ?: return null
        val resources = PHAssetResource.assetResourcesForAsset(asset).filterIsInstance<PHAssetResource>()
        val resource = resources.firstOrNull { it.type == PHAssetResourceTypePhoto }
            ?: resources.firstOrNull() ?: return null
        val options = PHAssetResourceRequestOptions().apply { networkAccessAllowed = true }
        val buffer = NSMutableData()
        return suspendCoroutine { continuation ->
            PHAssetResourceManager.defaultManager().requestDataForAssetResource(
                resource = resource,
                options = options,
                dataReceivedHandler = { chunk -> buffer.appendBytes(chunk.bytes, chunk.length) },
                completionHandler = { error ->
                    if (error != null) {
                        continuation.resumeWithException(
                            IllegalStateException("PhotoKit could not read asset: ${error.localizedDescription}"),
                        )
                    } else {
                        val length = buffer.length.toInt()
                        continuation.resume(if (length == 0) byteArrayOf() else buffer.bytes!!.readBytes(length))
                    }
                },
            )
        }
    }
}

private fun ByteArray.toNSData(): NSData =
    usePinned { pinned -> NSData.create(bytes = pinned.addressOf(0), length = size.toULong()) }

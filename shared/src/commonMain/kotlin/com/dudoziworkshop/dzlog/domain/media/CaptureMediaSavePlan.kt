package com.dudoziworkshop.dzlog.domain.media

import com.dudoziworkshop.dzlog.domain.counter.CounterScope
import com.dudoziworkshop.dzlog.domain.model.SaveMode
import com.dudoziworkshop.dzlog.domain.naming.buildCounterPath
import com.dudoziworkshop.dzlog.domain.naming.capturePhysicalSavePaths
import com.dudoziworkshop.dzlog.domain.preview.FinalCapturePreview
import com.dudoziworkshop.dzlog.domain.table.TablePatch

/** The roles and logical folders are common. Physical media IDs belong to each platform. */
enum class CaptureMediaRole { RESULT, ORIGINAL }

data class CaptureMediaTarget(
    val role: CaptureMediaRole,
    val relativePath: String,
    val displayName: String,
) {
    init {
        require(relativePath.startsWith("Pictures/DZlog/") && relativePath.endsWith("/"))
        require(displayName.isNotBlank() && displayName == displayName.substringAfterLast('/'))
        require(displayName != "." && displayName != "..")
    }
}

/**
 * A read-only decision made from an already synchronized FinalCapturePreview.
 * Never advance counters or mutate templates during planning.
 */
data class CaptureMediaSavePlan(
    val captureId: String,
    val usedCounter: Int,
    val counterScope: CounterScope,
    val patchAfterSuccess: TablePatch,
    val targets: List<CaptureMediaTarget>,
) {
    init {
        require(captureId.isNotBlank())
        require(usedCounter >= 1)
        require(targets.isNotEmpty())
        require(targets.map { it.role }.distinct().size == targets.size)
        require(targets.map { it.relativePath to it.displayName }.distinct().size == targets.size) {
            "Original and processed photos cannot overwrite the same physical destination."
        }
    }

    /** Fail before writing when any required image is missing or empty. */
    fun validatePayloads(jpegByRole: Map<CaptureMediaRole, ByteArray>) {
        require(jpegByRole.keys == targets.map { it.role }.toSet())
        require(jpegByRole.values.all { it.isNotEmpty() })
    }

    /** Only a full, matching save receipt permits the caller to commit [patchAfterSuccess]. */
    fun confirm(assets: List<PersistedCaptureAsset>): ConfirmedCaptureMediaSave =
        ConfirmedCaptureMediaSave(this, assets)
}

data class PersistedCaptureAsset(
    val target: CaptureMediaTarget,
    val assetId: String,
)

data class ConfirmedCaptureMediaSave(
    val plan: CaptureMediaSavePlan,
    val assets: List<PersistedCaptureAsset>,
) {
    init {
        require(assets.size == plan.targets.size)
        require(assets.map { it.target }.toSet() == plan.targets.toSet())
        require(assets.all { it.assetId.isNotBlank() })
        require(assets.map { it.assetId }.distinct().size == assets.size)
    }

    val patchAfterSuccess: TablePatch get() = plan.patchAfterSuccess
}

/**
 * Use existing Android naming/path rules as the sole source of truth.
 * iOS translates relativePath into Photos albums or sandbox locations in its adapter.
 */
fun buildCaptureMediaSavePlan(
    finalPreview: FinalCapturePreview,
    saveMode: SaveMode,
    captureId: String,
): CaptureMediaSavePlan {
    require(finalPreview.counterScope.relativePathKey ==
        buildCounterPath(finalPreview.relativePathPreview, saveMode)) {
        "Save mode must match the counter scope used to produce FinalCapturePreview."
    }
    val paths = capturePhysicalSavePaths(finalPreview.relativePathPreview, saveMode)
    val roles = when (saveMode) {
        SaveMode.ORIGINAL_ONLY -> listOf(CaptureMediaRole.ORIGINAL)
        SaveMode.WATERMARK_ONLY -> listOf(CaptureMediaRole.RESULT)
        SaveMode.BOTH -> listOf(CaptureMediaRole.RESULT, CaptureMediaRole.ORIGINAL)
    }
    return CaptureMediaSavePlan(
        captureId = captureId,
        usedCounter = finalPreview.usedCounter,
        counterScope = finalPreview.counterScope,
        patchAfterSuccess = finalPreview.tablePatch,
        targets = roles.zip(paths) { role, path ->
            CaptureMediaTarget(role, path, finalPreview.displayName)
        },
    )
}

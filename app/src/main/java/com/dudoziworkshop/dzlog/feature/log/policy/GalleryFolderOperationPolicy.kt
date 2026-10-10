package com.dudoziworkshop.dzlog.feature.log.policy

import com.dudoziworkshop.dzlog.domain.model.TableEditorSlotDraft
import com.dudoziworkshop.dzlog.domain.naming.sanitizeFilePart
import java.util.Locale

/**
 * Gallery filesystem operations never rewrite the camera's saved capture path.
 * The caller must require explicit user confirmation before proceeding when
 * the source is selected as (or is an ancestor of) a capture destination.
 */
enum class CapturePathImpact { NONE, POSSIBLE, MATCH }

object GalleryFolderOperationPolicy {
    /**
     * Preview real configured save-path slots from active and saved templates.
     * Dynamic slots can vary per shot; a conservative prefix is used instead
     * of pretending their current value represents all future photos.
     */
    fun captureImpact(
        sourceFolder: String,
        pathDrafts: List<List<TableEditorSlotDraft?>>,
    ): CapturePathImpact {
        val source = sourceFolder.trimEnd('/') + "/"
        require(source.startsWith(GalleryFolderIndexPolicy.ROOT))
        var possible = false
        for (drafts in pathDrafts) {
            val prefix = StringBuilder(GalleryFolderIndexPolicy.ROOT)
            var dynamic = false
            for (draft in drafts) {
                when (draft?.kind?.uppercase(Locale.ROOT)) {
                    null -> Unit
                    "MANUAL" -> {
                        val token = sanitizeFilePart(draft.manualText.orEmpty())
                        if (token.isNotBlank()) prefix.append(token).append('/')
                    }
                    else -> {
                        dynamic = true
                        break
                    }
                }
            }
            val path = prefix.toString()
            if (!dynamic) {
                if (path.startsWith(source)) return CapturePathImpact.MATCH
            } else if (source.startsWith(path) || path.startsWith(source)) {
                possible = true
            }
        }
        return if (possible) CapturePathImpact.POSSIBLE else CapturePathImpact.NONE
    }


    fun affectsCapturePath(
        sourceFolder: String,
        configuredCapturePaths: List<String>,
    ): Boolean {
        val source = sourceFolder.trimEnd('/') + "/"
        return configuredCapturePaths.any { configured ->
            val target = configured.trimEnd('/') + "/"
            target == source || target.startsWith(source)
        }
    }

    fun renamedPath(sourceFolder: String, rawName: String): String {
        val root = GalleryFolderIndexPolicy.ROOT
        val source = sourceFolder.trimEnd('/') + "/"
        require(source.startsWith(root) && source != root) {
            "DZlog 루트 폴더는 이름을 변경할 수 없습니다."
        }
        require(!source.trimEnd('/').endsWith("/original", ignoreCase = true)) {
            "원본사진 전용 폴더는 이름을 변경할 수 없습니다."
        }
        val name = GalleryFolderNamePolicy.normalize(rawName)
        val parent = requireNotNull(GalleryFolderIndexPolicy.parentOf(source))
        return parent + name + "/"
    }

    fun movedPath(sourceFolder: String, destinationFolder: String): String {
        validateMove(sourceFolder, destinationFolder)
        return destinationFolder.trimEnd('/') + "/" +
            sourceFolder.trimEnd('/').substringAfterLast('/') + "/"
    }

    fun validateMove(sourceFolder: String, destinationFolder: String) {
        val root = GalleryFolderIndexPolicy.ROOT
        val source = sourceFolder.trimEnd('/') + "/"
        val dest = destinationFolder.trimEnd('/') + "/"
        require(source.startsWith(root) && dest.startsWith(root) && source != root) {
            "DZlog 루트 폴더는 이동할 수 없습니다."
        }
        require(!source.trimEnd('/').endsWith("/original", ignoreCase = true)) {
            "원본사진 전용 폴더는 이동할 수 없습니다."
        }
        require(GalleryFolderIndexPolicy.parentOf(source) != dest) {
            "이미 같은 위치에 있는 폴더입니다."
        }
        require(!dest.startsWith(source)) {
            "폴더를 자기 자신 또는 자신의 하위 폴더로 이동할 수 없습니다."
        }
    }
}

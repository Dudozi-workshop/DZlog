package com.dudoziworkshop.dzlog.feature.log.policy

import com.dudoziworkshop.dzlog.domain.model.TableEditorSlotDraft
import com.dudoziworkshop.dzlog.domain.naming.sanitizeFilePart
import com.dudoziworkshop.dzlog.data.template.EditorWorkspace

/**
 * Gallery filesystem operations never rewrite the camera's saved capture path.
 * The caller must require explicit user confirmation before proceeding when
 * the source is selected as (or is an ancestor of) a capture destination.
 */
enum class CapturePathImpact { NONE, POSSIBLE, MATCH }

object GalleryFolderOperationPolicy {
    fun captureImpact(sourceFolder: String, workspace: EditorWorkspace): CapturePathImpact =
        captureImpact(sourceFolder, workspace.documents.map { it.template.templateState.pathSlotDrafts })

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
                when (draft?.kind?.uppercase()) {
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
        require(!GalleryFolderNamePolicy.isOriginalFolder(source)) {
            "원본사진 전용 폴더는 이름을 변경할 수 없습니다."
        }
        val name = GalleryFolderNamePolicy.normalize(rawName)
        val parent = requireNotNull(GalleryFolderIndexPolicy.parentOf(source))
        return parent + name + "/"
    }

    /** Preview and commit against the same iOS folder catalog, including empty folders. */
    fun checkedRenamedPath(sourceFolder: String, rawName: String, folders: Collection<String>): String {
        require(sourceFolder in folders) { "선택한 폴더를 찾을 수 없습니다." }
        val target = renamedPath(sourceFolder, rawName)
        require(rawName.trim().none(Char::isISOControl)) { "폴더 이름에 제어 문자를 사용할 수 없습니다." }
        if (target != sourceFolder) {
            require(folders.none { it.equals(target, ignoreCase = true) }) { "같은 이름의 폴더가 이미 있습니다." }
        }
        return target
    }

    fun movedPath(sourceFolder: String, destinationFolder: String) =
        GalleryFolderMovePolicy.movedPath(sourceFolder, destinationFolder)

    fun validateMove(sourceFolder: String, destinationFolder: String) =
        GalleryFolderMovePolicy.validateMove(sourceFolder, destinationFolder)
}

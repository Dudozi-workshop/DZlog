package com.dudoziworkshop.dzlog.feature.log.policy

import java.util.Locale

/** Literal find/replace. Never modify a photo extension or any unselected item. */
object GalleryBatchRenamePolicy {
    fun renamedName(name: String, find: String, replace: String, photo: Boolean): String {
        require(find.isNotEmpty()) { "찾을 문구를 입력해 주세요." }
        require(find.none { it == '/' || it == '\\' || it.code < 32 }) {
            "찾을 문구에 경로 구분자를 사용할 수 없습니다."
        }
        require(replace.none { it == '/' || it == '\\' || it == ':' || it == '*' ||
            it == '?' || it == '"' || it == '<' || it == '>' || it == '|' || it.code < 32 }) {
            "바꿀 문구에 파일명으로 사용할 수 없는 문자가 포함되어 있습니다."
        }
        if (find !in name) return name
        val dot = if (photo) name.lastIndexOf('.').takeIf { it > 0 } ?: name.length else name.length
        val stem = name.substring(0, dot)
        val suffix = name.substring(dot)
        val changed = stem.replace(find, replace) + suffix
        if (photo) {
            require(changed.substringBeforeLast('.', changed).isNotBlank() && changed.length <= 240) {
                "변경 후 파일 이름이 비어 있거나 너무 깁니다."
            }
        } else {
            GalleryFolderNamePolicy.normalize(changed)
        }
        return changed
    }

    /** Check against *all* existing names, including those not part of this operation. */
    fun requireNoCollisions(
        changes: List<Pair<String, String>>,
        existingNames: Collection<String>,
    ) {
        val oldNames = changes.map { it.first.lowercase(Locale.ROOT) }
        require(oldNames.distinct().size == oldNames.size) { "선택한 항목의 기존 이름이 중복됩니다." }
        val targetNames = changes.map { it.second.lowercase(Locale.ROOT) }
        require(targetNames.distinct().size == targetNames.size) {
            "변경 후 동일한 이름이 여러 개 생성됩니다."
        }
        val occupied = existingNames.mapTo(mutableSetOf()) { it.lowercase(Locale.ROOT) }
        for ((old, new) in changes) {
            if (!old.equals(new, ignoreCase = true) && new.lowercase(Locale.ROOT) in occupied) {
                throw IllegalArgumentException("변경 후 이름이 기존 파일 또는 폴더와 겹칩니다: $new")
            }
        }
    }
}

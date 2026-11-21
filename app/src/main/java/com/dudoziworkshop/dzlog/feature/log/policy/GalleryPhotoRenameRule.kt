package com.dudoziworkshop.dzlog.feature.log.policy

import com.dudoziworkshop.dzlog.domain.model.FILE_NAME_SLOT_COUNT
import com.dudoziworkshop.dzlog.domain.naming.NamingFormatDefaults.FILE_NAME_DELIMITER
import com.dudoziworkshop.dzlog.domain.naming.splitFileName

internal sealed interface GalleryPhotoRenameRule {
    val replacement: String

    data class Exact(val phrase: String, override val replacement: String) : GalleryPhotoRenameRule
    data class Position(val index: Int, override val replacement: String) : GalleryPhotoRenameRule
    data class Literal(val find: String, override val replacement: String) : GalleryPhotoRenameRule
    data class Merge(val phrases: Set<String>, override val replacement: String) : GalleryPhotoRenameRule

    fun validate() {
        require(replacement.none(::invalidFileCharacter)) { "파일명에 사용할 수 없는 문자가 있습니다." }
        if (this is Literal) {
            require(find.isNotBlank()) { "찾을 문구를 입력해 주세요." }
        } else {
            require(replacement.isNotBlank()) { "새 문구를 입력해 주세요." }
            require(FILE_NAME_DELIMITER !in replacement) { "새 구성 문구에는 구분자(_)를 사용할 수 없습니다." }
        }
        if (this is Merge) require(phrases.size >= 2) { "통합할 문구를 두 개 이상 선택해 주세요." }
    }
}

internal data class GalleryPhotoName(
    val prefix: String,
    val counter: String,
    val duplicate: String,
    val extension: String,
) {
    val protectedSuffix get() = counter + duplicate + extension
    val components: List<String>?
        get() = prefix.split(FILE_NAME_DELIMITER).takeIf { parts ->
            parts.size <= FILE_NAME_SLOT_COUNT && parts.none { it.isBlank() || it != it.trim() } &&
                (duplicate.isEmpty() || counter.isNotEmpty())
        }

    fun apply(rule: GalleryPhotoRenameRule): String {
        if (rule is GalleryPhotoRenameRule.Literal) {
            return withPrefix(prefix.replace(rule.find, rule.replacement))
        }
        val parts = requireNotNull(components) { "파일명 구성의 구분을 확인해 주세요." }
        val changed = when (rule) {
            is GalleryPhotoRenameRule.Exact -> parts.map { if (it == rule.phrase) rule.replacement else it }
            is GalleryPhotoRenameRule.Position -> parts.mapIndexed { index, part ->
                if (index == rule.index) rule.replacement else part
            }
            is GalleryPhotoRenameRule.Merge -> {
                require(parts.count { it in rule.phrases } <= 1) { "통합 대상 문구가 한 파일명에 여러 번 있습니다." }
                parts.map { if (it in rule.phrases) rule.replacement else it }
            }
            is GalleryPhotoRenameRule.Literal -> error("Literal rule handled above")
        }
        return withPrefix(changed.joinToString(FILE_NAME_DELIMITER))
    }

    fun withPrefix(newPrefix: String): String {
        if (newPrefix == prefix) return prefix + protectedSuffix
        require(newPrefix.isNotBlank() && newPrefix == newPrefix.trim() &&
            !newPrefix.startsWith('.') && !newPrefix.endsWith('.') &&
            newPrefix.none(::invalidFileCharacter)) { "변경 후 파일명을 확인해 주세요." }
        val name = newPrefix + protectedSuffix
        require(isValidGalleryPhotoNameLength(name)) { "변경 후 파일명이 너무 깁니다." }
        // A new trailing numeric component would otherwise be read as a counter on an older file.
        require(parse(name).protectedSuffix == protectedSuffix) { "카운터, 중복 번호 및 확장자는 바꿀 수 없습니다." }
        return name
    }

    companion object {
        private val duplicateSuffix = Regex(" \\(([0-9]+)\\)$")
        private val counterSuffix = Regex("${Regex.escape(FILE_NAME_DELIMITER)}[0-9]+$")

        fun parse(name: String): GalleryPhotoName {
            val (stem, extension) = splitFileName(name)
            val duplicate = duplicateSuffix.find(stem)
                ?.takeIf { (it.groupValues[1].toIntOrNull() ?: 0) >= 2 }?.value.orEmpty()
            val body = stem.removeSuffix(duplicate)
            val counter = counterSuffix.find(body)?.value.orEmpty()
            return GalleryPhotoName(body.removeSuffix(counter), counter, duplicate, extension)
        }
    }
}

internal fun isValidGalleryPhotoNameLength(name: String): Boolean =
    name.length <= 240 && name.toByteArray(Charsets.UTF_8).size <= 255

private fun invalidFileCharacter(character: Char): Boolean =
    character in "\\/:*?\"<>|" || character.isISOControl()

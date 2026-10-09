package com.dudoziworkshop.dzlog.feature.log.policy

/**
 * Only a single new directory name is accepted. Paths never come from text entry.
 * An "original" folder is reserved for paired unwatermarked captures.
 */
object GalleryFolderNamePolicy {
    fun normalize(input: String): String {
        val name = input.trim()
        require(name.isNotEmpty()) { "폴더 이름을 입력해 주세요." }
        require(name.length <= 80) { "폴더 이름은 80자 이하로 입력해 주세요." }
        require(name != "." && name != "..") { "사용할 수 없는 폴더 이름입니다." }
        require(!name.equals("original", ignoreCase = true)) {
            "original은 원본사진 보관용 이름입니다."
        }
        require(name.none { c ->
            c == '/' || c == '\\' || c == ':' || c == '*' || c == '?' ||
                c == '"' || c == '<' || c == '>' || c == '|' || c.code < 32
        }) { "폴더 이름에 사용할 수 없는 문자가 포함되어 있습니다." }
        require(!name.endsWith('.')) { "폴더 이름 끝에는 마침표를 사용할 수 없습니다." }
        return name
    }
}

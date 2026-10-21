package com.dudoziworkshop.dzlog.ui.table.mock

/** An explicit, one-time copy. Editing either rule later never changes the other. */
enum class SaveRuleCopyDirection {
    FILE_NAME_TO_PATH,
    PATH_TO_FILE_NAME,
}

fun canCopySaveRuleComposition(
    draft: MockSaveRulesDraft,
    direction: SaveRuleCopyDirection,
    validCellIds: Set<String>,
): Boolean {
    val source = when (direction) {
        SaveRuleCopyDirection.FILE_NAME_TO_PATH -> draft.fileNameItems
        SaveRuleCopyDirection.PATH_TO_FILE_NAME -> draft.pathItems
    }
    val capacity = when (direction) {
        SaveRuleCopyDirection.FILE_NAME_TO_PATH -> draft.pathItems.size
        SaveRuleCopyDirection.PATH_TO_FILE_NAME -> draft.fileNameItems.size
    }
    val nonEmpty = source.filterNotNull()
    return nonEmpty.isNotEmpty() &&
        nonEmpty.size <= capacity &&
        nonEmpty.all { item ->
            item.sourceType != MockRuleSourceType.CELL ||
                (!item.cellId.isNullOrBlank() && item.cellId in validCellIds)
        }
}

/** Copy into the current edit draft only, never into saved photos or templates. */
fun copySaveRuleComposition(
    draft: MockSaveRulesDraft,
    direction: SaveRuleCopyDirection,
    validCellIds: Set<String>,
): MockSaveRulesDraft {
    if (!canCopySaveRuleComposition(draft, direction, validCellIds)) return draft

    val source = when (direction) {
        SaveRuleCopyDirection.FILE_NAME_TO_PATH -> draft.fileNameItems
        SaveRuleCopyDirection.PATH_TO_FILE_NAME -> draft.pathItems
    }
    val copy = source.filterNotNull()
    val capacity = when (direction) {
        SaveRuleCopyDirection.FILE_NAME_TO_PATH -> draft.pathItems.size
        SaveRuleCopyDirection.PATH_TO_FILE_NAME -> draft.fileNameItems.size
    }
    val replacement = List(capacity) { index -> copy.getOrNull(index) }

    return when (direction) {
        SaveRuleCopyDirection.FILE_NAME_TO_PATH -> draft.copy(pathItems = replacement)
        SaveRuleCopyDirection.PATH_TO_FILE_NAME -> draft.copy(fileNameItems = replacement)
    }
}

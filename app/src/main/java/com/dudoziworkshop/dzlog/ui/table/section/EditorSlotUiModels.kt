package com.dudoziworkshop.dzlog.ui.table.section

/**
 * Shared filename/path slot models retained for compatibility with the
 * existing table structure and filename/path editor policies.
 * The retired LayoutTab screen state and action contracts are not used by V2.
 */
enum class FileNameSlotKind {
    CELL,
    FORMAT,
    MANUAL
}

enum class FileNameFormatType {
    DATE,
    TIME,
    COUNTER,
    ROTATING_TEXT
}

data class FileNameSlotUiItem(
    val kind: FileNameSlotKind,
    val label: String,
    val cellId: String? = null,
    val manualText: String? = null,
    val formatType: FileNameFormatType? = null
)

enum class PathSlotKind {
    CELL,
    FORMAT,
    MANUAL
}

enum class PathFormatType {
    DATE,
    TIME,
    ROTATING_TEXT
}

data class PathSlotUiItem(
    val kind: PathSlotKind,
    val label: String,
    val cellId: String? = null,
    val manualText: String? = null,
    val formatType: PathFormatType? = null
)

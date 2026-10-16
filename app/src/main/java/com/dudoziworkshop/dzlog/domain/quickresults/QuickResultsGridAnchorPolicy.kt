package com.dudoziworkshop.dzlog.domain.quickresults

import com.dudoziworkshop.dzlog.domain.gallery.GalleryGridColumnsPolicy

/**
 * Converts the first visible LazyColumn item to the corresponding item in a
 * reflowed quick-results list. Each segment header occupies one list item.
 * This is independent of Compose and does not alter the captured image data.
 */
object QuickResultsGridAnchorPolicy {
    data class Position(val itemIndex: Int, val scrollOffset: Int)

    private data class VisibleItem(val sectionKey: String, val photoCaptureIndices: List<Int>)

    private fun items(
        sections: List<QuickResultSection>,
        collapsedKeys: Set<String>,
        columns: Int,
    ): List<VisibleItem> = buildList {
        val size = GalleryGridColumnsPolicy.normalize(columns)
        sections.forEach { section ->
            add(VisibleItem(section.key, emptyList())) // header
            if (section.key !in collapsedKeys) {
                section.captures.chunked(size).forEach { row ->
                    add(VisibleItem(section.key, row.map { it.captureIndex }))
                }
            }
        }
    }

    fun remap(
        sections: List<QuickResultSection>,
        collapsedKeys: Set<String>,
        firstVisibleItemIndex: Int,
        firstVisibleScrollOffset: Int,
        oldColumns: Int,
        newColumns: Int,
    ): Position {
        val oldItems = items(sections, collapsedKeys, oldColumns)
        val newItems = items(sections, collapsedKeys, newColumns)
        val oldItem = oldItems.getOrNull(firstVisibleItemIndex)
            ?: return Position(0, 0)
        val anchorCaptureIndex = oldItem.photoCaptureIndices.firstOrNull()
        val matchingIndex = newItems.indexOfFirst { item ->
            item.sectionKey == oldItem.sectionKey &&
                if (anchorCaptureIndex == null) {
                    item.photoCaptureIndices.isEmpty()
                } else {
                    anchorCaptureIndex in item.photoCaptureIndices
                }
        }
        return Position(
            itemIndex = matchingIndex.takeIf { it >= 0 } ?: 0,
            scrollOffset = firstVisibleScrollOffset.coerceAtLeast(0),
        )
    }
}

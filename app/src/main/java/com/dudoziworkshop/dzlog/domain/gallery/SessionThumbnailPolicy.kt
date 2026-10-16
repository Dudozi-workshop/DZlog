package com.dudoziworkshop.dzlog.domain.gallery

/**
 * Choose the most recent readable photo from a continuous camera session.
 * Groups are oldest-first shutter commits; each group may contain a processed
 * result and its original. Folder changes never affect the selected thumbnail.
 *
 * MediaStore availability is supplied by the caller, so deleted/trashed photos
 * cannot be mistaken for still-present captures.
 */
object SessionThumbnailPolicy {
    fun <Id, Photo> newestAvailable(
        groupsOldestFirst: List<List<Id>>,
        lookup: (Id) -> Photo?,
        isOriginal: (Photo) -> Boolean,
    ): Photo? {
        for (group in groupsOldestFirst.asReversed()) {
            val found = group.distinct().mapNotNull(lookup)
            if (found.isNotEmpty()) {
                return found.firstOrNull { !isOriginal(it) } ?: found.first()
            }
        }
        return null
    }
}

package com.dudoziworkshop.dzlog.data.mediastore

/**
 * Small in-memory cache for immediate folder navigation.
 *
 * Cached results are rendered while MediaStore is revalidated asynchronously.
 * Nothing is persisted: app restarts and external storage changes always re-query.
 */
internal class GallerySnapshotCache(
    private val maxEntries: Int = 5,
    private val clockMillis: () -> Long = System::currentTimeMillis,
) {
    init { require(maxEntries > 0) }

    private data class Record(
        val snapshot: DzlogMediaStoreReader.GallerySnapshot,
        val timestamp: Long,
    )

    private val entries = object : LinkedHashMap<String, Record>(maxEntries, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Record>): Boolean =
            size > maxEntries
    }

    @Synchronized
    fun get(relativePath: String): DzlogMediaStoreReader.GallerySnapshot? =
        entries[relativePath]?.snapshot

    @Synchronized
    fun put(relativePath: String, snapshot: DzlogMediaStoreReader.GallerySnapshot) {
        entries[relativePath] = Record(snapshot, clockMillis())
    }

    @Synchronized
    fun invalidate(relativePath: String? = null) {
        if (relativePath == null) entries.clear()
        else entries.keys.removeAll { key ->
            key == relativePath ||
                key.startsWith(relativePath) ||
                relativePath.startsWith(key)
        }
    }
}

internal object GallerySnapshotMemory {
    val cache = GallerySnapshotCache()
}

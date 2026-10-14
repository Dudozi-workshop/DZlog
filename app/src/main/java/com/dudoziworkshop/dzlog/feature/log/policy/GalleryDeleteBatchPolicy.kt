package com.dudoziworkshop.dzlog.feature.log.policy

/** Keep each Android confirmation request small. */
object GalleryDeleteBatchPolicy {
    const val MAX_BATCH_SIZE = 100

    fun <T> batchAt(items: List<T>, offset: Int): List<T> {
        require(offset in 0..items.size)
        return items.drop(offset).take(MAX_BATCH_SIZE)
    }
}

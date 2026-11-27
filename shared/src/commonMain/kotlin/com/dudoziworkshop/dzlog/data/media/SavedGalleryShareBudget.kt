package com.dudoziworkshop.dzlog.data.media

/** Bound the first multi-file share slice while native resource reads return byte arrays. */
object SavedGalleryShareBudget {
    const val MAX_PHOTOS = 50
    const val MAX_BYTES = 64L * 1024 * 1024

    fun validateCount(count: Int) {
        require(count in 1..MAX_PHOTOS) { "한 번에 1~${MAX_PHOTOS}장을 선택해 주세요." }
    }

    fun appendBytes(currentBytes: Long, nextBytes: Int): Long {
        require(currentBytes in 0..MAX_BYTES && nextBytes > 0 && nextBytes.toLong() <= MAX_BYTES - currentBytes) {
            "공유할 사진의 합계가 64MB를 넘습니다. 사진을 나눠 선택해 주세요."
        }
        return currentBytes + nextBytes
    }
}

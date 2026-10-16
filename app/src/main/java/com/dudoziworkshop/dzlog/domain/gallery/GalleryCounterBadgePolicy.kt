package com.dudoziworkshop.dzlog.domain.gallery

/**
 * Presentation-only badge policy: never changes saved filenames or counter state.
 *
 * The input must be a verified capture counter, either captured at save time or
 * validated using the appropriate naming rule. Do not infer a counter from an
 * arbitrary trailing numeric filename token (which could be a date).
 */
object GalleryCounterBadgePolicy {
    fun labelForVerifiedCounter(counter: Int?, enabled: Boolean): String? {
        if (!enabled) return null
        return counter?.takeIf { it > 0 }?.toString()
    }
}

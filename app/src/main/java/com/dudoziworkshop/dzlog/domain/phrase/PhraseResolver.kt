package com.dudoziworkshop.dzlog.domain.phrase

import com.dudoziworkshop.dzlog.domain.model.RotatingPhraseSet

/**
 * 문구 선택 정책 전용 resolver.
 *
 * 정책:
 * - 통합/문구별 모드와 무관하게 progress 기반으로 현재 문구를 선택한다.
 * - 저장 성공 전에는 progress를 바꾸지 않고, 성공 후 nextProgressCursor를 반영한다.
 */
object PhraseResolver {

    data class PhraseSelection(
        val text: String,
        val index: Int,
        val nextProgressCursor: Int,
    )

    fun resolve(
        phraseSet: RotatingPhraseSet?,
        every: Int,
        progressCursor: Int
    ): PhraseSelection? {
        if (phraseSet == null || phraseSet.items.isEmpty()) return null

        val effectiveEvery = every.coerceAtLeast(1)
        val safeProgress = progressCursor.coerceAtLeast(1)
        val index = ((safeProgress - 1) / effectiveEvery) % phraseSet.items.size
        return PhraseSelection(
            text = phraseSet.items[index],
            index = index,
            nextProgressCursor = safeProgress + 1,
        )
    }
}

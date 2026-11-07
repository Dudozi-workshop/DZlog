package com.dudoziworkshop.dzlog.domain.quickresults

/**
 * Only exact shutter-linked photos may be deleted by Quick Results. Never
 * infer a second image from a matching name, storage folder or sequence.
 */
object QuickResultsCaptureDeletePolicy {
    fun mediaUrisForCapture(
        displayedUri: String,
        captures: List<QuickResultCapture>,
    ): List<String> {
        val matching = captures.filter { it.displayedPhoto.uri == displayedUri }
        require(matching.size == 1) {
            "촬영 기록이 변경됐습니다. 빠른 결과 목록을 다시 확인해 주세요."
        }
        val capture = matching.single()
        val targetUris = listOfNotNull(capture.result?.uri, capture.original?.uri)
        require(targetUris.isNotEmpty() && targetUris.size == targetUris.distinct().size) {
            "연결된 사진 정보가 올바르지 않습니다."
        }
        return targetUris
    }
}

package com.dudoziworkshop.dzlog.feature.settings.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class SettingsTutorialV2CatalogTest {
    @Test
    fun `guide follows actual settings menu order without Pro`() {
        assertEquals(
            listOf(
                "권한 관리", "사진 품질", "플로팅 보조 셔터",
                "촬영 효과음 및 진동", "저장경로 변경 시 삭제 경고",
                "작업 환경 내보내기", "작업 환경 가져오기",
                "사용법 및 도움말", "도움 주신 분들",
            ),
            SETTINGS_GUIDE_ITEMS.map { it.title },
        )
        assertEquals(4, SETTINGS_GUIDE_ITEMS.map { it.section }.distinct().size)
        assertFalse(SETTINGS_GUIDE_ITEMS.any { it.title.contains("Pro") || it.title.contains("구매") })
    }
}

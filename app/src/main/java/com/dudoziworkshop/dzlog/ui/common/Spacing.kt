package com.dudoziworkshop.dzlog.ui.common

import androidx.compose.ui.unit.dp

/**
 * DZlog UI spacing tokens (집 기준 v1.0)
 *
 * 원칙:
 * - dp 숫자 직접 사용 금지
 * - spacing은 역할 기반으로만 선택
 *
 * 역할 매핑:
 * - Inner   : 카드/옵션 내부 간격
 * - Section : 섹션 padding
 */
object Spacing {
    val Inner = 12.dp
    val Section = 16.dp
}

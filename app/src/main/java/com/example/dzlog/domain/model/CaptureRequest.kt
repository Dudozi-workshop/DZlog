package com.example.dzlog.domain.model

import com.example.dzlog.domain.table.ResolvedCell
import com.example.dzlog.domain.watermark.WatermarkBuilder.WatermarkCell

data class CaptureRequest(
    // 프로젝트 = 저장 경로(Pictures/DZlog/...)
    // group1/group2 값으로 buildGalleryRelativePath()를 통해 최종 경로를 결정함.
    val group1: String,
    val group2: String,

    // 파일명은 그룹과 무관
    val displayName: String,

    // 단일 데이터 소스: ResolvePlan 결과
    val resolvedCells: List<ResolvedCell>,
    val watermarkCells: List<WatermarkCell>,

    // 저장 모드
    val saveMode: SaveMode,

    // 촬영 비율(프리뷰와 저장 일치)
    val captureAspect: CaptureAspect,

    // 표 템플릿(레이아웃 정보)
    val tableTemplate: TableTemplateState,

    // 워터마크 렌더 입력
    val watermark: WatermarkConfig
)

data class WatermarkConfig(
    val showLabel: Boolean,
    val anchor: WatermarkTableAnchor,
    val offsetXRatio: Int,
    val offsetYRatio: Int,
    val tableHeightRatio: Int,
    val tableWidthRatio: Int,
    val tableBgAlpha: Int,
    val bgStyle: Int, // 0=BLACK, 1=WHITE, 2=TRANSPARENT
    val labelScale: Int,
    val valueScale: Int
)

package com.dudoziworkshop.dzlog.domain.model

import com.dudoziworkshop.dzlog.domain.table.ResolvedCell
import com.dudoziworkshop.dzlog.domain.watermark.WatermarkBuilder.WatermarkCell

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

    // 사진 품질 모드(촬영/저장 품질 정책)
    val photoQualityMode: PhotoQualityMode = PhotoQualityMode.BALANCED,

    // 표 템플릿(레이아웃 정보)
    val tableTemplate: TableTemplateState,

    // contentRect 기준 usable 세로 경계 비율(0..1). 저장 크롭/오버레이 정렬 공유용
    val usableTopRatio: Float = 0f,
    val usableBottomRatio: Float = 1f,

    // 워터마크 렌더 입력
    val watermark: WatermarkConfig
)

data class WatermarkConfig(
    val anchor: WatermarkTableAnchor,
    val offsetXRatio: Int,
    val offsetYRatio: Int,
    val boundsOffsetX10000: Int = 0,
    val boundsOffsetY10000: Int = 0,
    val tableHeightRatio: Int,
    val tableWidthRatio: Int,
    val tableBgAlpha: Int,
    val bgStyle: Int, // 0=BLACK, 1=WHITE, 2=TRANSPARENT
    val valueScale: Int,
    val textColorMode: Int, // 0=AUTO, 1=MANUAL
    val manualTextColor: Int, // 0=WHITE, 1=BLACK
    val textAlign: Int, // 0=LEFT, 1=CENTER, 2=RIGHT
    val gridEnabled: Boolean = true,
    val rotationCwDeg: Int = 0
)

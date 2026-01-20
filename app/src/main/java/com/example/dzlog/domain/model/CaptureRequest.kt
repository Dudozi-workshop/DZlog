package com.example.dzlog.domain.model

data class CaptureRequest(
    // 폴더 결정(=Group)
    val projectKey: String,
    val group1: String,
    val group2: String,

    // 파일명은 그룹과 무관
    val displayNameBase: String,

    // 카운터(촬영 시점 값)
    val counter: Int,
    val counterDigits: Int,

    // 저장 모드
    val saveMode: SaveMode,

    // 워터마크 렌더 입력
    val watermark: WatermarkConfig
)

data class WatermarkConfig(
    val showLabel: Boolean,
    val showDate: Boolean,
    val showTime: Boolean,
    val datePattern: String,
    val timePattern: String,
    val templatePreset: WatermarkTemplatePreset,
    val gridPreset: WatermarkGridPreset,
    val anchor: WatermarkTableAnchor,
    val offsetXRatio: Int,
    val offsetYRatio: Int,
    val tableHeightRatio: Int,
    val tableWidthRatio: Int,
    val tableBgAlpha: Int,
    val labelScale: Int,
    val valueScale: Int,
    val memo1: String,
    val memo2: String,
    val memo3: String,
    val emptyPolicy: EmptyValuePolicy,      // ✅ 이제 com.example.dzlog.EmptyValuePolicy 를 가리킴
    val emptyCustomText: String,

    // 템플릿에 들어갈 값들
    val treatment: String,
    val strain: String,
    val folder2Text: String
)

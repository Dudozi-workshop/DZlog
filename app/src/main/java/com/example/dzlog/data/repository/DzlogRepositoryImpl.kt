package com.example.dzlog.data.repository

import android.content.Context
import android.graphics.BitmapFactory
import androidx.camera.core.ImageCapture
import androidx.core.content.ContextCompat
import com.example.dzlog.data.mediastore.MediaStoreSaver
import com.example.dzlog.domain.model.CaptureRequest
import com.example.dzlog.domain.model.LogEntry
import com.example.dzlog.domain.model.SaveMode
import com.example.dzlog.watermark.WatermarkRenderer
import java.io.File
import androidx.camera.core.ImageCaptureException


class DzlogRepositoryImpl(
    private val saver: MediaStoreSaver,
    private val counterSync: com.example.dzlog.data.counter.CounterSync,
    private val watermarkRenderer: WatermarkRenderer
) : DzlogRepository {

    override fun buildRelativePath(projectKey: String, group1: String, group2: String): String {
        return "Pictures/DZlog/$projectKey/$group1/$group2/"
    }

    override fun buildOriginalRelativePath(projectKey: String, group1: String, group2: String): String {
        return "Pictures/DZlog/$projectKey/$group1/$group2/original/"
    }

    override fun captureAndSave(
        context: Context,
        imageCapture: ImageCapture,
        request: CaptureRequest,
        onDone: (LogEntry) -> Unit,
        onFail: (String) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(context)

        // ✅ 임시 파일로 촬영 → Bitmap 디코딩 → MediaStoreSaver.saveJpeg로 최종 저장
        val tmpFile = kotlin.runCatching {
            File.createTempFile("dzlog_", ".jpg", context.cacheDir)
        }.getOrElse {
            onFail("임시파일 생성 실패: ${it.message}")
            return
        }

        val outputOptions = ImageCapture.OutputFileOptions.Builder(tmpFile).build()

        imageCapture.takePicture(
            outputOptions,
            executor,
            object : ImageCapture.OnImageSavedCallback {

                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    try {
                        val originalBmp = BitmapFactory.decodeFile(tmpFile.absolutePath)
                            ?: throw IllegalStateException("촬영 이미지 디코딩 실패")

                        val baseRel = buildRelativePath(request.projectKey, request.group1, request.group2)
                        val origRel = buildOriginalRelativePath(request.projectKey, request.group1, request.group2)

                        val counterText = request.counter.toString().padStart(request.counterDigits, '0')
                        val displayName = ensureJpg("${request.displayNameBase}_$counterText")

                        when (request.saveMode) {
                            SaveMode.WATERMARK_ONLY -> {
                                val wmBmp = watermarkRenderer.renderTable(
                                    originalBmp = originalBmp,
                                    cells = buildResolvedCells(request),
                                    rows = request.watermark.gridPreset.rows,
                                    cols = request.watermark.gridPreset.cols,
                                    showLabel = request.watermark.showLabel,
                                    anchor = request.watermark.anchor,
                                    offsetXRatio = request.watermark.offsetXRatio,
                                    offsetYRatio = request.watermark.offsetYRatio,
                                    tableHeightRatio = request.watermark.tableHeightRatio,
                                    tableWidthRatio = request.watermark.tableWidthRatio,
                                    bgAlpha = request.watermark.tableBgAlpha,
                                    labelScale = request.watermark.labelScale,
                                    valueScale = request.watermark.valueScale
                                )

                                val saved = saver.saveJpeg(
                                    context = context,
                                    bitmap = wmBmp,
                                    displayName = displayName,
                                    relativePath = baseRel
                                )

                                tmpFile.delete()

                                onDone(
                                    LogEntry(
                                        mediaStoreId = saved.mediaStoreId,
                                        contentUri = saved.uri,
                                        createdAt = System.currentTimeMillis(),
                                        projectKey = request.projectKey,
                                        group1 = request.group1,
                                        group2 = request.group2
                                    )
                                )
                            }

                            SaveMode.BOTH -> {
                                // 1) 워터마크 먼저(대표 파일)
                                val wmBmp = watermarkRenderer.renderTable(
                                    originalBmp = originalBmp,
                                    cells = buildResolvedCells(request),
                                    rows = request.watermark.gridPreset.rows,
                                    cols = request.watermark.gridPreset.cols,
                                    showLabel = request.watermark.showLabel,
                                    anchor = request.watermark.anchor,
                                    offsetXRatio = request.watermark.offsetXRatio,
                                    offsetYRatio = request.watermark.offsetYRatio,
                                    tableHeightRatio = request.watermark.tableHeightRatio,
                                    tableWidthRatio = request.watermark.tableWidthRatio,
                                    bgAlpha = request.watermark.tableBgAlpha,
                                    labelScale = request.watermark.labelScale,
                                    valueScale = request.watermark.valueScale
                                )

                                val savedWm = saver.saveJpeg(
                                    context = context,
                                    bitmap = wmBmp,
                                    displayName = displayName,
                                    relativePath = baseRel
                                )

                                // 2) 원본은 original/ 하위 (실패해도 워터마크는 이미 저장됨)
                                kotlin.runCatching {
                                    saver.saveJpeg(
                                        context = context,
                                        bitmap = originalBmp,
                                        displayName = displayName,
                                        relativePath = origRel
                                    )
                                }

                                tmpFile.delete()

                                onDone(
                                    LogEntry(
                                        mediaStoreId = savedWm.mediaStoreId,
                                        contentUri = savedWm.uri,
                                        createdAt = System.currentTimeMillis(),
                                        projectKey = request.projectKey,
                                        group1 = request.group1,
                                        group2 = request.group2
                                    )
                                )
                            }

                            SaveMode.ORIGINAL_ONLY -> {
                                val saved = saver.saveJpeg(
                                    context = context,
                                    bitmap = originalBmp,
                                    displayName = displayName,
                                    relativePath = origRel
                                )

                                tmpFile.delete()

                                onDone(
                                    LogEntry(
                                        mediaStoreId = saved.mediaStoreId,
                                        contentUri = saved.uri,
                                        createdAt = System.currentTimeMillis(),
                                        projectKey = request.projectKey,
                                        group1 = request.group1,
                                        group2 = request.group2
                                    )
                                )
                            }
                        }
                    } catch (e: Exception) {
                        tmpFile.delete()
                        onFail(e.message ?: "captureAndSave 실패")
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    tmpFile.delete()
                    onFail(exception.message ?: "촬영 실패")
                }
            }
        )
    }
    // =====================================================
    // ✅ 워터마크 표에 들어갈 ResolvedCell 만들기
    // - 기존 resolveCellsFromTemplate 흐름을 Repository에서 호출
    // =====================================================
    // =====================================================
// ✅ 워터마크 표에 들어갈 ResolvedCell 만들기 (MVP 안정판)
// - 템플릿/CellDef 없이 바로 cells 구성
// =====================================================
    private fun buildResolvedCells(request: CaptureRequest): List<com.example.dzlog.domain.model.ResolvedCell> {
        val wm = request.watermark

        fun applyEmpty(raw: String): String {
            val v = raw.trim()
            if (v.isNotEmpty()) return v
            return when (wm.emptyPolicy) {
                com.example.dzlog.domain.model.EmptyValuePolicy.BLANK -> ""
                com.example.dzlog.domain.model.EmptyValuePolicy.DASH -> "-"
                com.example.dzlog.domain.model.EmptyValuePolicy.CUSTOM -> wm.emptyCustomText
            }
        }

        val counterText = request.counter.toString().padStart(request.counterDigits, '0')

        val now = java.util.Date()
        val dateText =
            if (wm.showDate) java.text.SimpleDateFormat(wm.datePattern, java.util.Locale.getDefault()).format(now) else ""
        val timeText =
            if (wm.showTime) java.text.SimpleDateFormat(wm.timePattern, java.util.Locale.getDefault()).format(now) else ""

        // ✅ 표에 넣을 "후보"들 (필요하면 순서/라벨 바꾸면 됨)
        val pairs: List<Pair<String, String>> = listOf(
            "Treatment" to wm.treatment,
            "Strain" to wm.strain,
            "Folder2" to wm.folder2Text,
            "Counter" to counterText,
            "Date" to dateText,
            "Time" to timeText,
            "Memo1" to wm.memo1,
            "Memo2" to wm.memo2,
            "Memo3" to wm.memo3
        )

        // ✅ rows*cols 만큼만 채우고, 부족하면 빈칸
        val need = (wm.gridPreset.rows * wm.gridPreset.cols).coerceAtLeast(1)

        val baseCells = pairs.map { (label, value) ->
            com.example.dzlog.domain.model.ResolvedCell(
                label = label,
                valueText = applyEmpty(value)
            )
        }

        return if (baseCells.size >= need) {
            baseCells.take(need)
        } else {
            baseCells + List(need - baseCells.size) {
                com.example.dzlog.domain.model.ResolvedCell(label = "", valueText = "")
            }
        }
    }


    private fun ensureJpg(name: String): String {
        val n = name.trim()
        return if (n.endsWith(".jpg", true) || n.endsWith(".jpeg", true)) n else "$n.jpg"
    }
}

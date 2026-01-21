package com.example.dzlog.data.repository

import android.content.Context
import android.graphics.BitmapFactory
import androidx.camera.core.ImageCapture
import androidx.core.content.ContextCompat
import com.example.dzlog.data.mediastore.MediaStoreSaver
import com.example.dzlog.domain.model.CaptureRequest
import com.example.dzlog.domain.model.LogEntry
import com.example.dzlog.domain.model.SaveMode
import com.example.dzlog.domain.watermark.resolveCellsFromTemplate
import com.example.dzlog.domain.watermark.templateForPreset
import com.example.dzlog.watermark.WatermarkRenderer
import java.io.File
import androidx.camera.core.ImageCaptureException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date


class DzlogRepositoryImpl(
    private val saver: MediaStoreSaver,
    private val counterSync: com.example.dzlog.data.counter.CounterSync,
    private val watermarkRenderer: WatermarkRenderer
) : DzlogRepository {

    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

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
                    ioScope.launch {
                        val result = runCatching {
                            val originalBmp = BitmapFactory.decodeFile(tmpFile.absolutePath)
                                ?: throw IllegalStateException("촬영 이미지 디코딩 실패")

                            val baseRel = buildRelativePath(request.projectKey, request.group1, request.group2)
                            val origRel = buildOriginalRelativePath(request.projectKey, request.group1, request.group2)

                            val counterText = request.counter.toString().padStart(request.counterDigits, '0')
                            val displayName = ensureJpg("${request.displayNameBase}_$counterText")
                            val capturedAt = Date()
                            val cells = buildResolvedCells(request, capturedAt)

                            when (request.saveMode) {
                                SaveMode.WATERMARK_ONLY -> {
                                    val wmBmp = watermarkRenderer.renderTable(
                                        originalBmp = originalBmp,
                                        cells = cells,
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

                                    LogEntry(
                                        mediaStoreId = saved.mediaStoreId,
                                        contentUri = saved.uri,
                                        createdAt = System.currentTimeMillis(),
                                        projectKey = request.projectKey,
                                        group1 = request.group1,
                                        group2 = request.group2
                                    )
                                }

                                SaveMode.BOTH -> {
                                    // 1) 워터마크 먼저(대표 파일)
                                    val wmBmp = watermarkRenderer.renderTable(
                                        originalBmp = originalBmp,
                                        cells = cells,
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

                                    LogEntry(
                                        mediaStoreId = savedWm.mediaStoreId,
                                        contentUri = savedWm.uri,
                                        createdAt = System.currentTimeMillis(),
                                        projectKey = request.projectKey,
                                        group1 = request.group1,
                                        group2 = request.group2
                                    )
                                }

                                SaveMode.ORIGINAL_ONLY -> {
                                    val saved = saver.saveJpeg(
                                        context = context,
                                        bitmap = originalBmp,
                                        displayName = displayName,
                                        relativePath = origRel
                                    )

                                    LogEntry(
                                        mediaStoreId = saved.mediaStoreId,
                                        contentUri = saved.uri,
                                        createdAt = System.currentTimeMillis(),
                                        projectKey = request.projectKey,
                                        group1 = request.group1,
                                        group2 = request.group2
                                    )
                                }
                            }
                        }

                        tmpFile.delete()

                        withContext(Dispatchers.Main) {
                            result.onSuccess { entry ->
                                onDone(entry)
                            }.onFailure { error ->
                                onFail(error.message ?: "captureAndSave 실패")
                            }
                        }
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
    private fun buildResolvedCells(
        request: CaptureRequest,
        capturedAt: Date
    ): List<com.example.dzlog.domain.model.ResolvedCell> {
        val wm = request.watermark
        val counterText = request.counter.toString().padStart(request.counterDigits, '0')

        val template = templateForPreset(
            preset = wm.templatePreset,
            grid = wm.gridPreset,
            memo1 = wm.memo1,
            memo2 = wm.memo2,
            memo3 = wm.memo3
        )

        return resolveCellsFromTemplate(
            template = template,
            treatment = wm.treatment,
            strain = wm.strain,
            folder2Text = wm.folder2Text,
            counterText = counterText,
            capturedAt = capturedAt,
            showDate = wm.showDate,
            showTime = wm.showTime,
            datePattern = wm.datePattern,
            timePattern = wm.timePattern,
            emptyPolicy = wm.emptyPolicy,
            emptyCustomText = wm.emptyCustomText
        )
    }


    private fun ensureJpg(name: String): String {
        val n = name.trim()
        return if (n.endsWith(".jpg", true) || n.endsWith(".jpeg", true)) n else "$n.jpg"
    }
}
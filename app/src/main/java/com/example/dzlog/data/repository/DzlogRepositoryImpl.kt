package com.example.dzlog.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.camera.core.ImageCapture
import androidx.core.content.ContextCompat
import androidx.exifinterface.media.ExifInterface
import com.example.dzlog.data.mediastore.MediaStoreSaver
import com.example.dzlog.domain.model.CaptureRequest
import com.example.dzlog.domain.model.CaptureAspect
import com.example.dzlog.domain.model.LogEntry
import com.example.dzlog.domain.model.SaveMode
import com.example.dzlog.watermark.WatermarkRenderer
import com.example.dzlog.watermark.renderWatermarkForRequest
import java.io.File
import java.io.FileOutputStream
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
        return buildGalleryBasePath(group1, group2)
    }

    override fun buildOriginalRelativePath(
        projectKey: String,
        group1: String,
        group2: String
    ): String {
        return "${buildGalleryBasePath(group1, group2)}original/"
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

        val metadata = ImageCapture.Metadata().apply {
            isReversedHorizontal = false
            isReversedVertical = false
        }
        val outputOptions = ImageCapture.OutputFileOptions.Builder(tmpFile)
            .setMetadata(metadata)
            .build()

        imageCapture.takePicture(
            outputOptions,
            executor,
            object : ImageCapture.OnImageSavedCallback {

                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    ioScope.launch {
                        val result = runCatching {
                            val decodedBmp = BitmapFactory.decodeFile(tmpFile.absolutePath)
                                ?: throw IllegalStateException("촬영 이미지 디코딩 실패")
                            val exif = ExifInterface(tmpFile)
                            val orientedBmp = applyExifOrientation(decodedBmp, exif)
                            val originalBmp = cropToAspect(orientedBmp, request.captureAspect)

                            val baseRel = buildRelativePath(
                                request.projectKey,
                                request.group1,
                                request.group2
                            )
                            val origRel = buildOriginalRelativePath(
                                request.projectKey,
                                request.group1,
                                request.group2
                            )

                            val displayName = request.displayName
                            val capturedAt = Date()

                            when (request.saveMode) {
                                SaveMode.WATERMARK_ONLY -> {
                                    val wmBmp = renderWatermarkForRequest(
                                        renderer = watermarkRenderer,
                                        originalBmp = originalBmp,
                                        request = request,
                                        capturedAt = capturedAt
                                    )

                                    val saved = kotlin.runCatching {
                                        saver.saveJpeg(
                                            context = context,
                                            bitmap = wmBmp,
                                            displayName = displayName,
                                            relativePath = baseRel
                                        )
                                    }.getOrNull()
                                    LogEntry(
                                        mediaStoreId = saved?.mediaStoreId ?: -1L,
                                        contentUri = saved?.uri ?: Uri.EMPTY,
                                        displayName = saved?.displayName ?: displayName,
                                        isNameAdjusted = saved?.isNameAdjusted ?: false,
                                        createdAt = System.currentTimeMillis(),
                                        projectKey = request.projectKey,
                                        group1 = request.group1,
                                        group2 = request.group2
                                    )
                                }

                                SaveMode.BOTH -> {
                                    // 1) 워터마크 먼저(대표 파일)
                                    val wmBmp = renderWatermarkForRequest(
                                        renderer = watermarkRenderer,
                                        originalBmp = originalBmp,
                                        request = request,
                                        capturedAt = capturedAt
                                    )

                                    val savedWm = kotlin.runCatching {
                                        saver.saveJpeg(
                                            context = context,
                                            bitmap = wmBmp,
                                            displayName = displayName,
                                            relativePath = baseRel
                                        )
                                    }.getOrNull()

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
                                        mediaStoreId = savedWm?.mediaStoreId ?: -1L,
                                        contentUri = savedWm?.uri ?: Uri.EMPTY,
                                        displayName = savedWm?.displayName ?: displayName,
                                        isNameAdjusted = savedWm?.isNameAdjusted ?: false,
                                        createdAt = System.currentTimeMillis(),
                                        projectKey = request.projectKey,
                                        group1 = request.group1,
                                        group2 = request.group2
                                    )
                                }

                                SaveMode.ORIGINAL_ONLY -> {
                                    val saved = kotlin.runCatching {
                                        saver.saveJpeg(
                                            context = context,
                                            bitmap = originalBmp,
                                            displayName = displayName,
                                            relativePath = origRel
                                        )
                                    }.getOrNull()

                                    LogEntry(
                                        mediaStoreId = saved?.mediaStoreId ?: -1L,
                                        contentUri = saved?.uri ?: Uri.EMPTY,
                                        displayName = saved?.displayName ?: displayName,
                                        isNameAdjusted = saved?.isNameAdjusted ?: false,
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

    private fun applyExifOrientation(source: Bitmap, exif: ExifInterface): Bitmap {
        val orientation = exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.postScale(-1f, 1f)
            }

            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f)
                matrix.postScale(-1f, 1f)
            }

            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            else -> return source
        }
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    private fun cropToAspect(source: Bitmap, aspect: CaptureAspect): Bitmap {
        val targetRatio = aspect.w.toFloat() / aspect.h.toFloat()
        val srcWidth = source.width
        val srcHeight = source.height
        if (srcWidth == 0 || srcHeight == 0) return source

        val srcRatio = srcWidth.toFloat() / srcHeight.toFloat()
        if (kotlin.math.abs(srcRatio - targetRatio) < 0.001f) {
            return source
        }

        val (cropWidth, cropHeight) = if (srcRatio > targetRatio) {
            val height = srcHeight
            val width = (height * targetRatio).toInt().coerceAtMost(srcWidth)
            width to height
        } else {
            val width = srcWidth
            val height = (width / targetRatio).toInt().coerceAtMost(srcHeight)
            width to height
        }

        val left = ((srcWidth - cropWidth) / 2f).toInt().coerceAtLeast(0)
        val top = ((srcHeight - cropHeight) / 2f).toInt().coerceAtLeast(0)

        return Bitmap.createBitmap(source, left, top, cropWidth, cropHeight)
    }

    private fun buildGalleryBasePath(group1: String, group2: String): String {
        val g1 = group1.trim()
        val g2 = group2.trim()
        return if (g1.isNotEmpty() && g2.isNotEmpty()) {
            "Pictures/DZlog/$g1/$g2/"
        } else {
            "Pictures/DZlog/"
        }
    }
}
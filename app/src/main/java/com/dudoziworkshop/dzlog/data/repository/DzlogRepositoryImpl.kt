package com.dudoziworkshop.dzlog.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.RectF
import android.net.Uri
import android.util.Log
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.core.content.ContextCompat
import androidx.core.graphics.scale
import androidx.exifinterface.media.ExifInterface
import com.dudoziworkshop.dzlog.data.log.LogEntity
import com.dudoziworkshop.dzlog.data.log.LogRepository
import com.dudoziworkshop.dzlog.data.mediastore.MediaStoreSaver
import com.dudoziworkshop.dzlog.domain.camera.computeAnchoredCaptureRect
import com.dudoziworkshop.dzlog.domain.model.CaptureRequest
import com.dudoziworkshop.dzlog.domain.model.LogEntry
import com.dudoziworkshop.dzlog.domain.model.PhotoQualityMode
import com.dudoziworkshop.dzlog.domain.model.SaveMode
import com.dudoziworkshop.dzlog.watermark.WatermarkRenderer
import com.dudoziworkshop.dzlog.watermark.renderWatermarkForRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File


class DzlogRepositoryImpl(
    private val saver: MediaStoreSaver,
    private val watermarkRenderer: WatermarkRenderer
) : DzlogRepository {

    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
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
                            val originalBmp = cropToAspect(orientedBmp, request)

                            val baseRel = normalizeCaptureBaseRelativePath(request.relativePath)
                            val origRel = appendOriginalCaptureDirectory(baseRel)

                            val displayName = request.displayName
                            val qualityMode = request.photoQualityMode
                            val jpegQuality = qualityMode.jpegQuality
                            Log.d(
                                "CounterReadback",
                                "captureAndSave request saveMode=${request.saveMode}, baseRel=$baseRel, origRel=$origRel, displayName=$displayName"
                            )
                            val saved = CaptureMediaSaveCoordinator.save(
                                mode = request.saveMode,
                                saveWatermark = {
                                    val wmBmp = renderWatermarkForRequest(watermarkRenderer, originalBmp, request)
                                    saver.saveJpeg(context, applyPhotoQualityPolicy(wmBmp, qualityMode),
                                        displayName, baseRel, jpegQuality)
                                },
                                saveOriginal = {
                                    saver.saveJpeg(context, applyPhotoQualityPolicy(originalBmp, qualityMode),
                                        displayName, origRel, jpegQuality)
                                },
                                deleteSaved = { uri -> saver.deleteByUri(context, uri) },
                            )
                            val entry = LogEntry(
                                mediaStoreId = saved.primary.mediaStoreId,
                                contentUri = saved.primary.uri,
                                savedContentUris = saved.files.map { it.uri },
                                displayName = saved.primary.displayName,
                                isNameAdjusted = saved.primary.isNameAdjusted,
                                createdAt = System.currentTimeMillis(),
                                group1 = request.group1,
                                group2 = request.group2,
                            )
                            if (request.saveMode != SaveMode.ORIGINAL_ONLY) {
                                // The media is already committed; optional log failure must not
                                // turn a successful capture into a failed counter/phrase advance.
                                try {
                                    insertLogEntry(context, entry, saved.primary.uri,
                                        saved.primary.displayName, baseRel)
                                } catch (error: Exception) {
                                    Log.e("DZlogCapture", "사진 저장 후 로그 기록 실패", error)
                                }
                            }
                            entry
                        }

                        tmpFile.delete()

                        withContext(Dispatchers.Main) {
                            result.onSuccess { entry ->
                                onDone(entry)
                            }.onFailure { error ->
                                val message = error.message ?: "captureAndSave 실패"
                                onFail(if (error.suppressed.isNotEmpty()) {
                                    "$message · 일부 사진을 정리하지 못했습니다. 갤러리를 확인하세요."
                                } else message)
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


    private suspend fun insertLogEntry(
        context: Context,
        entry: LogEntry,
        uri: Uri?,
        fileName: String,
        relativePath: String
    ) {
        val imageUri = uri?.toString().orEmpty()
        if (imageUri.isBlank()) return
        val log = LogEntity(
            createdAt = entry.createdAt,
            imageUri = imageUri,
            fileName = fileName,
            relativePath = relativePath,
            templateId = null,
            templateName = null,
            valuesJson = null
        )
        LogRepository.getInstance(context).insert(log)
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


    private fun applyPhotoQualityPolicy(source: Bitmap, mode: PhotoQualityMode): Bitmap {
        val maxLongEdge = mode.maxLongEdgePx ?: return source
        val width = source.width
        val height = source.height
        if (width <= 0 || height <= 0) return source

        val longEdge = maxOf(width, height)
        if (longEdge <= maxLongEdge) return source

        val scale = maxLongEdge.toFloat() / longEdge.toFloat()
        val targetWidth = (width * scale).toInt().coerceAtLeast(1)
        val targetHeight = (height * scale).toInt().coerceAtLeast(1)
        return source.scale(width = targetWidth, height = targetHeight, filter = true)
    }

    private fun cropToAspect(source: Bitmap, request: CaptureRequest): Bitmap {
        if (source.width == 0 || source.height == 0) return source

        val fullRect = RectF(0f, 0f, source.width.toFloat(), source.height.toFloat())
        val framing = computeAnchoredCaptureRect(
            contentRect = fullRect,
            captureAspectRatio = request.captureAspect.ratioF
        )
        val targetRect = framing.captureRect

        val left = targetRect.left.toInt().coerceIn(0, source.width - 1)
        val top = targetRect.top.toInt().coerceIn(0, source.height - 1)
        val right = kotlin.math.ceil(targetRect.right.toDouble()).toInt().coerceIn(left + 1, source.width)
        val bottom = kotlin.math.ceil(targetRect.bottom.toDouble()).toInt().coerceIn(top + 1, source.height)

        val width = (right - left).coerceAtLeast(1)
        val height = (bottom - top).coerceAtLeast(1)

        Log.d(
            "DZlogCrop",
            "crop aspect=${request.captureAspect.label} src=${source.width}x${source.height} rect=($left,$top)-($right,$bottom) anchorY=${framing.anchorY}"
        )

        return Bitmap.createBitmap(source, left, top, width, height)
    }
}


internal fun normalizeCaptureBaseRelativePath(relativePath: String): String {
    val trimmed = relativePath.trim()
    if (trimmed.isBlank()) return "Pictures/DZlog/"
    return if (trimmed.endsWith('/')) trimmed else "$trimmed/"
}

internal fun appendOriginalCaptureDirectory(baseRelativePath: String): String {
    val normalized = normalizeCaptureBaseRelativePath(baseRelativePath)
    return if (normalized.endsWith("original/")) normalized else "${normalized}original/"
}


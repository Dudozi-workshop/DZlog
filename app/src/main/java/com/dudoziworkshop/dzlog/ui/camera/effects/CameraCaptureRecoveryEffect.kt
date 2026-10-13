package com.dudoziworkshop.dzlog.ui.camera.effects

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.dudoziworkshop.dzlog.data.repository.CapturePendingStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Passive diagnostic only. Never saves, deletes, or retries capture jobs on resume.
 * The actual queue must be separately enabled after capture pipeline verification.
 */
@Composable
internal fun CameraCaptureRecoveryEffect() {
    val context = LocalContext.current
    LaunchedEffect(context) {
        val inspection = withContext(Dispatchers.IO) {
            runCatching { CapturePendingStore(context).inspectRecovery() }
        }
        inspection.onSuccess { report ->
            val requiresReview = report.needsReview.size + report.orphanFiles.size +
                report.corruptJournals.size + report.completedAwaitingApply.size +\n                    report.completedAwaitingApply.size
            Log.i(
                "DZlogCaptureRecovery",
                "ready=${report.ready.size} requiresReview=$requiresReview"
            )
            if (requiresReview > 0 || report.ready.isNotEmpty()) {
                Toast.makeText(
                    context,
                    "완료되지 않은 촬영 ${requiresReview + report.ready.size}건이 보관되어 있습니다. 자동 재저장은 하지 않습니다.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }.onFailure { error ->
            Log.e("DZlogCaptureRecovery", "Unable to inspect pending captures", error)
            Toast.makeText(
                context, "촬영 대기 파일 상태를 확인하지 못했습니다.", Toast.LENGTH_LONG
            ).show()
        }
    }
}

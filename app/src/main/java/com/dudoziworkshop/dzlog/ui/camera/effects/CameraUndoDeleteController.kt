package com.dudoziworkshop.dzlog.ui.camera.effects

import android.app.Activity
import android.app.RecoverableSecurityException
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import com.dudoziworkshop.dzlog.feature.capture.policy.UndoCapturePolicy
import com.dudoziworkshop.dzlog.feature.capture.policy.classifyUndoDeletion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal data class UndoDeleteController(
    val isBusy: Boolean,
    val deleteLatest: (stack: MutableList<List<Uri>>) -> Unit,
)

@Composable
internal fun rememberUndoDeleteController(
    context: Context,
    onCommitted: () -> Unit,
    onRestore: (targetUris: List<Uri>) -> Unit,
): UndoDeleteController {
    val scope = rememberCoroutineScope()
    val latestOnCommitted = rememberUpdatedState(onCommitted)
    val latestOnRestore = rememberUpdatedState(onRestore)

    var pendingUndoDeleteUris by remember { mutableStateOf<List<Uri>?>(null) }
    var retryPermissionDeleteUri by remember { mutableStateOf<Uri?>(null) }
    var isDeleting by remember { mutableStateOf(false) }

    val undoDeleteLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val pendingUris = pendingUndoDeleteUris ?: return@rememberLauncherForActivityResult
        val needsRetry = retryPermissionDeleteUri != null
        pendingUndoDeleteUris = null
        retryPermissionDeleteUri = null

        if (result.resultCode != Activity.RESULT_OK) {
            try {
                latestOnRestore.value(pendingUris)
            } finally {
                isDeleting = false
            }
        } else if (!needsRetry) {
            // Android 11+ MediaStore.createDeleteRequest performs deletion itself.
            try {
                latestOnCommitted.value()
            } finally {
                isDeleting = false
            }
        } else {
            // Android 10 RecoverableSecurityException grants access only.
            // The app must issue the delete call again after user consent.
            scope.launch {
                try {
                    val retryAttempts = withContext(Dispatchers.IO) {
                        pendingUris.map { uri ->
                            uri to runCatching { context.contentResolver.delete(uri, null, null) > 0 }
                        }
                    }
                    val outcome = classifyUndoDeletion(retryAttempts)
                    if (outcome.deleted.isNotEmpty()) latestOnCommitted.value()
                    if (outcome.remaining.isNotEmpty()) latestOnRestore.value(outcome.remaining)
                } finally {
                    isDeleting = false
                }
            }
        }
    }

    fun launchScopedDeleteRequest(targetUris: List<Uri>): Boolean {
        if (targetUris.isEmpty()) return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val pendingIntent = MediaStore.createDeleteRequest(context.contentResolver, targetUris)
            pendingUndoDeleteUris = targetUris
            undoDeleteLauncher.launch(IntentSenderRequest.Builder(pendingIntent.intentSender).build())
            return true
        }
        return false
    }

    suspend fun performUndoDelete(targetUris: List<Uri>) {
        if (targetUris.isEmpty()) {
            isDeleting = false
            return
        }

        // A capture may have multiple output files. Never restore successfully
        // deleted URIs to the Undo stack if only part of the batch failed.
        val attempts = withContext(Dispatchers.IO) {
            targetUris.map { uri ->
                uri to runCatching { context.contentResolver.delete(uri, null, null) > 0 }
            }
        }
        val outcome = classifyUndoDeletion(attempts)
        val failed = attempts.filter { (_, result) -> result.getOrNull() != true }

        try {
            if (outcome.deleted.isNotEmpty()) latestOnCommitted.value()

            val recoverable = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                failed.firstOrNull { (_, result) ->
                    result.exceptionOrNull() is RecoverableSecurityException
                }
            } else null

            val remaining = outcome.remaining
            if (recoverable == null) {
                if (remaining.isNotEmpty()) latestOnRestore.value(remaining)
                return
            }

            // Android 10 recovery grants deletion of the specific failed URI,
            // not necessarily permission for every file in the capture batch.
            val recoverableUri = recoverable.first
            val otherFailures = remaining.filter { it != recoverableUri }
            if (otherFailures.isNotEmpty()) latestOnRestore.value(otherFailures)

            pendingUndoDeleteUris = listOf(recoverableUri)
            retryPermissionDeleteUri = recoverableUri
            runCatching {
                val security = recoverable.second.exceptionOrNull() as RecoverableSecurityException
                undoDeleteLauncher.launch(
                    IntentSenderRequest.Builder(security.userAction.actionIntent.intentSender).build()
                )
            }.onFailure {
                pendingUndoDeleteUris = null
                retryPermissionDeleteUri = null
                latestOnRestore.value(listOf(recoverableUri))
            }
        } finally {
            // Keep the button disabled while awaiting Android's confirmation UI.
            if (pendingUndoDeleteUris == null) isDeleting = false
        }
    }

    fun deleteLatest(stack: MutableList<List<Uri>>) {
        // Reserve the batch only after checking busy. Two rapid taps must not
        // pop two captures while the first deletion is still pending.
        if (isDeleting || pendingUndoDeleteUris != null) return
        val targetUris = UndoCapturePolicy.consumeLatestCapture(stack)
        if (targetUris.isEmpty()) return

        isDeleting = true
        val launched = runCatching { launchScopedDeleteRequest(targetUris) }.getOrElse {
            pendingUndoDeleteUris = null
            retryPermissionDeleteUri = null
            isDeleting = false
            latestOnRestore.value(targetUris)
            return
        }
        if (launched) return
        scope.launch { performUndoDelete(targetUris) }
    }

    return UndoDeleteController(
        isBusy = isDeleting,
        deleteLatest = ::deleteLatest,
    )
}


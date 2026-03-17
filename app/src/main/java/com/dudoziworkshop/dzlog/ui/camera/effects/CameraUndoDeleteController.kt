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
import kotlinx.coroutines.launch

internal data class UndoDeleteController(
    val pendingUris: List<Uri>?,
    val delete: (targetUris: List<Uri>) -> Unit,
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

    val undoDeleteLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val pendingUris = pendingUndoDeleteUris ?: return@rememberLauncherForActivityResult
        if (result.resultCode == Activity.RESULT_OK) {
            latestOnCommitted.value()
        } else {
            latestOnRestore.value(pendingUris)
        }
        pendingUndoDeleteUris = null
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

    fun performUndoDelete(targetUris: List<Uri>) {
        if (targetUris.isEmpty()) return

        val deletedAll = runCatching {
            targetUris.all { uri ->
                context.contentResolver.delete(uri, null, null) > 0
            }
        }.getOrElse { throwable ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && throwable is RecoverableSecurityException) {
                pendingUndoDeleteUris = targetUris
                undoDeleteLauncher.launch(
                    IntentSenderRequest.Builder(throwable.userAction.actionIntent.intentSender).build()
                )
                return
            }
            false
        }

        if (!deletedAll) {
            latestOnRestore.value(targetUris)
            return
        }

        latestOnCommitted.value()
    }

    fun delete(targetUris: List<Uri>) {
        if (pendingUndoDeleteUris != null) return
        if (launchScopedDeleteRequest(targetUris)) return
        scope.launch { performUndoDelete(targetUris) }
    }

    return UndoDeleteController(
        pendingUris = pendingUndoDeleteUris,
        delete = ::delete,
    )
}


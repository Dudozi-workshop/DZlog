package com.dudoziworkshop.dzlog.ui.log

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dudoziworkshop.dzlog.data.mediastore.GalleryPhotoRenamer
import com.dudoziworkshop.dzlog.data.mediastore.GalleryPhotoRenameSnapshot
import com.dudoziworkshop.dzlog.feature.log.policy.GalleryPhotoRenamePlan
import com.dudoziworkshop.dzlog.feature.log.policy.GalleryPhotoRenameResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Gallery selection stays local to its screen. The active operation survives Activity recreation. */
internal class GalleryPhotoRenameSession : ViewModel() {
    var selectedIds by mutableStateOf(emptySet<Long>())
        private set
    var snapshot by mutableStateOf<GalleryPhotoRenameSnapshot?>(null)
        private set
    var result by mutableStateOf<GalleryPhotoRenameResult?>(null)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var busy by mutableStateOf(false)
        private set
    var pendingPermission by mutableStateOf<GalleryPhotoRenamePlan?>(null)
        private set

    fun start(ids: Set<Long>, renamer: GalleryPhotoRenamer) {
        if (busy || selectedIds.isNotEmpty() || ids.isEmpty()) return
        selectedIds = ids
        runOnStorage({ renamer.prepare(ids) }, "사진을 불러오지 못했습니다.") { snapshot = it }
    }

    fun requestPermission(plan: GalleryPhotoRenamePlan) {
        error = null
        pendingPermission = plan
    }

    fun permissionFailed(message: String) {
        pendingPermission = null
        error = message
    }

    fun execute(renamer: GalleryPhotoRenamer, plan: GalleryPhotoRenamePlan) {
        if (busy) return
        pendingPermission = null
        runOnStorage({ renamer.rename(plan) }, "파일 이름 변경에 실패했습니다.") { result = it }
    }

    fun dismiss() {
        if (busy || pendingPermission != null) return
        selectedIds = emptySet()
        snapshot = null
        result = null
        error = null
    }

    private fun <T> runOnStorage(operation: suspend () -> T, fallback: String, onSuccess: (T) -> Unit) {
        busy = true
        error = null
        viewModelScope.launch {
            try {
                onSuccess(withContext(Dispatchers.IO) { operation() })
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                if (snapshot == null) selectedIds = emptySet()
                error = failure.message ?: fallback
            } finally {
                busy = false
            }
        }
    }
}

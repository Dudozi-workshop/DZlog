package com.dudoziworkshop.dzlog.ui.project

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dudoziworkshop.dzlog.data.project.CaptureProjectStore
import com.dudoziworkshop.dzlog.data.repository.CapturePendingStore
import com.dudoziworkshop.dzlog.domain.project.CaptureProjectRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext

/**
 * Project metadata operations. Creating, renaming and browsing never select a
 * capture target; switching or completing the selected project must not race
 * a durable in-flight save or a pending receipt review.
 */
internal class ProjectManagementViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application.applicationContext
    private val store = CaptureProjectStore(context)
    private val pending = CapturePendingStore(context)

    val registry: StateFlow<CaptureProjectRegistry> = store.changes.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = CaptureProjectRegistry(),
    )

    suspend fun create(name: String, initialTemplateId: String?): String =
        store.createProject(name, initialTemplateId).projects.last().id

    suspend fun rename(projectId: String, name: String) {
        require(name.trim().isNotBlank()) { "프로젝트 이름을 입력해 주세요." }
        store.rename(projectId, name)
    }

    suspend fun canChangeCaptureTarget(): Boolean = withContext(Dispatchers.IO) {
        val recovery = pending.inspectRecovery()
        recovery.ready.isEmpty() && recovery.needsReview.isEmpty() &&
            recovery.completedAwaitingApply.isEmpty() && recovery.orphanFiles.isEmpty() &&
            recovery.corruptJournals.isEmpty()
    }

    suspend fun select(projectId: String?) {
        check(canChangeCaptureTarget()) { "사진 저장 완료 후 프로젝트를 변경할 수 있습니다." }
        store.selectProject(projectId)
    }

    suspend fun rememberTemplate(templateId: String) {
        store.rememberTemplate(templateId)
    }

    suspend fun complete(projectId: String) {
        if (store.load().activeId == projectId) {
            check(canChangeCaptureTarget()) { "사진 저장 완료 후 프로젝트를 완료할 수 있습니다." }
        }
        store.completeProject(projectId)
    }

    suspend fun reopen(projectId: String) {
        // Reopening a completed project changes only its status, not the camera.
        store.reopenProject(projectId)
    }
}

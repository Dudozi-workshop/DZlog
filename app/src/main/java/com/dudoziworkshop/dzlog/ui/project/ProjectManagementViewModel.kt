package com.dudoziworkshop.dzlog.ui.project

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.dudoziworkshop.dzlog.data.project.CaptureProjectStore
import com.dudoziworkshop.dzlog.data.repository.CapturePendingStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Non-destructive project lifecycle. Photo deletion is deliberately not exposed
 * until the Android-specific, verified deletion workflow is re-integrated.
 */
internal class ProjectManagementViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application.applicationContext
    private val projects = CaptureProjectStore(context)
    private val pending = CapturePendingStore(context)

    suspend fun create(name: String, templateId: String?): String =
        projects.createProject(name, templateId).projects.last().id

    suspend fun canChangeCaptureTarget(): Boolean = withContext(Dispatchers.IO) {
        val report = pending.inspectRecovery()
        report.ready.isEmpty() && report.needsReview.isEmpty() &&
            report.completedAwaitingApply.isEmpty() && report.orphanFiles.isEmpty() &&
            report.corruptJournals.isEmpty()
    }

    suspend fun select(projectId: String?) {
        check(canChangeCaptureTarget()) { "사진 저장 완료 후 프로젝트를 변경할 수 있습니다." }
        projects.selectProject(projectId)
    }

    suspend fun rememberTemplate(templateId: String) {
        projects.rememberTemplate(templateId)
    }

    suspend fun rename(projectId: String, name: String) {
        require(name.trim().isNotBlank()) { "프로젝트 이름을 입력해 주세요." }
        projects.rename(projectId, name)
    }

    suspend fun complete(projectId: String) {
        if (projects.load().activeId == projectId) {
            check(canChangeCaptureTarget()) { "사진 저장 완료 후 프로젝트를 완료할 수 있습니다." }
        }
        projects.completeProject(projectId)
    }

    suspend fun reopen(projectId: String) {
        projects.reopenProject(projectId)
    }
}

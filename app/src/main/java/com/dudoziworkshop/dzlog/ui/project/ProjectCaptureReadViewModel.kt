package com.dudoziworkshop.dzlog.ui.project

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import com.dudoziworkshop.dzlog.data.mediastore.DzlogMediaStoreReader
import com.dudoziworkshop.dzlog.data.project.CaptureRecordRoomStore
import com.dudoziworkshop.dzlog.data.project.LegacyCaptureMigration
import com.dudoziworkshop.dzlog.data.project.LegacyCapturePhoto
import com.dudoziworkshop.dzlog.domain.model.MediaImageItem
import com.dudoziworkshop.dzlog.domain.project.CaptureProjectRegistry
import com.dudoziworkshop.dzlog.domain.project.CaptureRecord
import com.dudoziworkshop.dzlog.domain.project.ProjectCaptureReadPolicy
import com.dudoziworkshop.dzlog.domain.project.VisibleProjectCaptures
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/** Shared read result for the Home preview and accumulated project results. */
internal data class ProjectCaptureReadResult(
    val visible: VisibleProjectCaptures,
    val photos: List<MediaImageItem>,
    val latestPhoto: MediaImageItem?,
)

/**
 * One MediaStore scan per requested entry/refresh. Legacy v1 import is safe to
 * repeat, does not delete the source DataStore, and cannot claim another
 * project's already indexed media.
 */
internal class ProjectCaptureReadViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application.applicationContext
    private val records = CaptureRecordRoomStore(context)
    private val migration = LegacyCaptureMigration(records)

    fun observeProject(projectId: String): Flow<List<CaptureRecord>> =
        records.observeProject(projectId)

    suspend fun readProject(
        registry: CaptureProjectRegistry,
        projectId: String,
    ): ProjectCaptureReadResult = withContext(Dispatchers.IO) {
        check(registry.find(projectId) != null) { "Unknown project" }
        val photos = DzlogMediaStoreReader(context.contentResolver)
            .loadImagesUnderPrefix("Pictures/DZlog/")
        val report = migration.importAvailable(
            registry,
            photos.map { photo ->
                LegacyCapturePhoto(
                    uri = photo.uri.toString(),
                    relativePath = photo.relativePath,
                    dateAddedSeconds = photo.dateAddedSeconds,
                )
            },
        )
        if (report.conflicts.isNotEmpty()) {
            Log.w("DZlogProjectRead", "Legacy media ownership conflicts: " + report.conflicts.size)
        }
        // No guessed file entries: only currently available photos may appear.
        val byUri = photos.associateBy { it.uri.toString() }
        val visible = ProjectCaptureReadPolicy.visible(
            records = records.forProject(projectId),
            availableUris = byUri.keys,
        )
        ProjectCaptureReadResult(
            visible = visible,
            photos = photos,
            latestPhoto = visible.latestUri?.let(byUri::get),
        )
    }
}

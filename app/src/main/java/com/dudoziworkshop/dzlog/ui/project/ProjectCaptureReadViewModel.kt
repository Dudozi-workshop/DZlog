package com.dudoziworkshop.dzlog.ui.project

import android.app.Application
import android.net.Uri
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

/** One indexed project summary. Images are verified through a single MediaStore scan. */
internal data class ProjectCaptureSummary(
    val captureCount: Int = 0,
    val lastCapturedAtMillis: Long? = null,
    val recentPhotos: List<MediaImageItem> = emptyList(),
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

    /**
     * Camera-only lightweight refresh: Room already owns the ordered captures.
     * Query individual saved media URIs for the latest readable thumbnail,
     * avoiding a full Pictures/DZlog scan for every shutter press.
     */
    suspend fun latestIndexedProjectPhoto(records: List<CaptureRecord>): MediaImageItem? =
        withContext(Dispatchers.IO) {
            val uriGroups = records.asReversed().map { record ->
                listOfNotNull(record.resultUri, record.originalUri).map(Uri::parse)
            }
            DzlogMediaStoreReader(context.contentResolver).loadLatestImageInSession(uriGroups)
        }

    /** One scan for the whole management list, not a scan per project row. */
    suspend fun readSummaries(
        registry: CaptureProjectRegistry,
    ): Map<String, ProjectCaptureSummary> = withContext(Dispatchers.IO) {
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
            Log.w("DZlogProjectRead", "Legacy media conflicts: ${report.conflicts.size}")
        }
        val byUri = photos.associateBy { it.uri.toString() }
        registry.projects.associate { project ->
            val found = records.forProject(project.id).mapNotNull { record ->
                val photo = record.resultUri?.let(byUri::get) ?: record.originalUri?.let(byUri::get)
                photo?.let { record.capturedAtMillis to it }
            }
            project.id to ProjectCaptureSummary(
                captureCount = found.size,
                lastCapturedAtMillis = found.firstOrNull()?.first,
                recentPhotos = found.take(3).map { it.second },
            )
        }
    }

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

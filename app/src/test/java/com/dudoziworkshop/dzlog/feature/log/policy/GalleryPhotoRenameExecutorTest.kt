package com.dudoziworkshop.dzlog.feature.log.policy

import android.net.Uri
import com.dudoziworkshop.dzlog.domain.model.MediaImageItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GalleryPhotoRenameExecutorTest {
    private val resultPath = "Pictures/DZlog/A/"
    private val originalPath = "Pictures/DZlog/A/original/"

    private fun photo(id: Long, name: String, path: String = resultPath) =
        MediaImageItem(id, Uri.parse("content://media/external/images/media/$id"), name, path, 1)

    private fun literal(selected: List<MediaImageItem>, all: List<MediaImageItem> = selected) =
        GalleryPhotoRenamePolicy.plan(GalleryPhotoRenamePolicy.groups(selected, all), all,
            GalleryPhotoRenameRule.Literal("old", "new"))

    @Test fun a_new_blocker_excludes_only_the_affected_photo_group() {
        val selected = listOf(photo(1, "old_001.jpg"), photo(2, "old_002.jpg"))
        val storage = Storage(selected + photo(3, "new_001.jpg"))
        val result = GalleryPhotoRenameExecutor(storage).execute(literal(selected))
        assertEquals(listOf(GalleryRenameOutcome.Excluded, GalleryRenameOutcome.Success), result.rows.map { it.outcome })
        assertEquals(setOf(2L), result.renamedIds)
        assertEquals("old_001.jpg", storage.names[1L]?.name)
        assertEquals("new_001.jpg", storage.names[3L]?.name)
    }

    @Test fun pair_is_rolled_back_when_the_original_write_fails_and_other_rows_continue() {
        val selected = listOf(photo(1, "old_001.jpg"), photo(3, "old_002.jpg"))
        val all = selected + photo(2, "old_001.jpg", originalPath)
        val storage = Storage(all) { id, _ -> id != 2L }
        val result = GalleryPhotoRenameExecutor(storage).execute(literal(selected, all))
        assertEquals(listOf(GalleryRenameOutcome.Failure, GalleryRenameOutcome.Success), result.rows.map { it.outcome })
        assertEquals("old_001.jpg", storage.names[1L]?.name)
        assertEquals("old_001.jpg", storage.names[2L]?.name)
        assertEquals("new_002.jpg", storage.names[3L]?.name)
    }

    @Test fun failed_rollback_reports_the_actual_remaining_name_and_leaves_other_photos_intact() {
        val selected = listOf(photo(1, "old_001.jpg"))
        val all = selected + photo(2, "old_001.jpg", originalPath)
        val storage = Storage(all) { id, name -> id == 1L && name == "new_001.jpg" }
        val result = GalleryPhotoRenameExecutor(storage).execute(literal(selected, all))
        assertEquals(GalleryRenameOutcome.RecoveryNeeded, result.rows.single().outcome)
        assertEquals(mapOf(1L to "new_001.jpg"), result.rows.single().recoveryNames)
        assertTrue(result.renamedIds.isEmpty())
    }

    @Test fun ready_name_swaps_are_staged_then_verified() {
        val selected = listOf(photo(1, "A_001.jpg"), photo(2, "B_001.jpg"))
        val planned = GalleryPhotoRenamePolicy.plan(GalleryPhotoRenamePolicy.groups(selected, selected), selected,
            GalleryPhotoRenameRule.Literal("unused", ""),
            mapOf(1L to GalleryRenameResolution.Manual("B"), 2L to GalleryRenameResolution.Manual("A")))
        val storage = Storage(selected)
        val result = GalleryPhotoRenameExecutor(storage).execute(planned)
        assertEquals(setOf(1L, 2L), result.renamedIds)
        assertEquals("B_001.jpg", storage.names[1L]?.name)
        assertEquals("A_001.jpg", storage.names[2L]?.name)
    }

    @Test fun a_failed_dependency_does_not_allow_the_next_photo_to_take_its_old_name() {
        val selected = listOf(photo(1, "A_001.jpg"), photo(2, "B_001.jpg"))
        val planned = GalleryPhotoRenamePolicy.plan(GalleryPhotoRenamePolicy.groups(selected, selected), selected,
            GalleryPhotoRenameRule.Literal("unused", ""),
            mapOf(1L to GalleryRenameResolution.Manual("B"), 2L to GalleryRenameResolution.Manual("C")))
        val storage = Storage(selected) { id, _ -> id != 2L }
        val result = GalleryPhotoRenameExecutor(storage).execute(planned)
        assertEquals(listOf(GalleryRenameOutcome.Excluded, GalleryRenameOutcome.Failure), result.rows.map { it.outcome })
        assertEquals("A_001.jpg", storage.names[1L]?.name)
        assertEquals("B_001.jpg", storage.names[2L]?.name)
    }

    private class Storage(
        photos: List<MediaImageItem>,
        private val canWrite: (Long, String) -> Boolean = { _, _ -> true },
    ) : GalleryPhotoRenameStorage {
        val names = photos.associate { it.id to GalleryStoredPhotoName(it.displayName, renamePath(it.relativePath)) }
            .toMutableMap()

        override fun read(photo: MediaImageItem) = names[photo.id]

        override fun folderFiles(path: String): List<Pair<Long, String>> =
            names.filterValues { it.path == renamePath(path) }.map { (id, metadata) -> id to metadata.name }

        override fun update(photo: MediaImageItem, name: String): Boolean {
            if (!canWrite(photo.id, name)) return false
            check(folderFiles(photo.relativePath).none { (id, current) ->
                id != photo.id && current.equals(name, ignoreCase = true)
            })
            names[photo.id] = GalleryStoredPhotoName(name, renamePath(photo.relativePath))
            return true
        }
    }
}

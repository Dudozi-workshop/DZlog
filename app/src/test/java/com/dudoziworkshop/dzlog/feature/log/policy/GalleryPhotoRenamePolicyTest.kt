package com.dudoziworkshop.dzlog.feature.log.policy

import android.net.Uri
import com.dudoziworkshop.dzlog.domain.model.MediaImageItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GalleryPhotoRenamePolicyTest {
    private val resultPath = "Pictures/DZlog/A/"
    private val originalPath = "Pictures/DZlog/A/original/"

    private fun photo(id: Long, name: String, path: String = resultPath) =
        MediaImageItem(id, Uri.parse("content://media/external/images/media/$id"), name, path, 1)

    private fun plan(
        selected: List<MediaImageItem>,
        rule: GalleryPhotoRenameRule,
        all: List<MediaImageItem> = selected,
        resolutions: Map<Long, GalleryRenameResolution> = emptyMap(),
    ) = GalleryPhotoRenamePolicy.plan(GalleryPhotoRenamePolicy.groups(selected, all), all, rule, resolutions)

    @Test fun exact_phrase_requires_a_full_component_and_uses_the_saved_order() {
        val rows = plan(
            listOf(photo(1, "꽃눈_Draper_001.jpg"), photo(2, "Draper_꽃눈_002.JPG"), photo(3, "Draper_꽃눈1_003.png")),
            GalleryPhotoRenameRule.Exact("꽃눈", "잎눈"),
        ).rows
        assertEquals(listOf("잎눈_Draper_001.jpg", "Draper_잎눈_002.JPG", "Draper_꽃눈1_003.png"),
            rows.map { it.finalName })
        assertEquals(GalleryRenameStatus.Unchanged, rows[2].status)
    }

    @Test fun position_uses_only_existing_components_and_flags_visible_boundary_ambiguity() {
        val rows = plan(
            listOf(
                photo(1, "꽃눈_Draper_006.jpg"), photo(2, "Draper_002.jpg"),
                photo(3, "Draper__꽃눈_003.jpg"), photo(4, "A_B_C_D_E_F_004.jpg"),
            ),
            GalleryPhotoRenameRule.Position(1, "잎눈"),
        ).rows
        assertEquals("꽃눈_잎눈_006.jpg", rows[0].finalName)
        assertEquals(listOf(GalleryRenameStatus.Ready, GalleryRenameStatus.Unchanged,
            GalleryRenameStatus.Check, GalleryRenameStatus.Check), rows.map { it.status })
    }

    @Test fun literal_keeps_the_counter_duplicate_number_and_extension() {
        val rule = GalleryPhotoRenameRule.Literal("005", "99")
        val rows = plan(
            listOf(photo(1, "Draper005_005 (2).JPG"), photo(2, "Draper_005.png")), rule,
        ).rows
        assertEquals("Draper99_005 (2).JPG", rows[0].finalName)
        assertEquals(GalleryRenameStatus.Unchanged, rows[1].status)
    }

    @Test fun merge_is_single_pass_and_repeated_matches_need_review() {
        val rows = plan(
            listOf(
                photo(1, "Draper_꽃눈1_001.jpg"), photo(2, "Draper_꽃봉오리_002.jpg"),
                photo(3, "Draper_꽃눈_003.jpg"), photo(4, "꽃눈1_꽃봉오리_004.jpg"),
            ),
            GalleryPhotoRenameRule.Merge(setOf("꽃눈1", "꽃봉오리", "꽃눈"), "꽃눈"),
        ).rows
        assertEquals(listOf("Draper_꽃눈_001.jpg", "Draper_꽃눈_002.jpg"), rows.take(2).map { it.finalName })
        assertEquals(GalleryRenameStatus.Unchanged, rows[2].status)
        assertEquals(GalleryRenameStatus.Check, rows[3].status)
    }

    @Test fun both_selected_members_count_as_one_and_same_names_in_unrelated_folders_are_allowed() {
        val result = photo(1, "wrong_001.jpg")
        val original = photo(2, "wrong_001.jpg", originalPath)
        val unrelated = photo(3, "right_001.jpg", "Pictures/DZlog/B/")
        val considered = plan(listOf(result, original), GalleryPhotoRenameRule.Exact("wrong", "right"),
            listOf(result, original, unrelated))
        assertEquals(listOf(1L, 2L), considered.writablePhotos.map { it.id })
        assertEquals("right_001.jpg", considered.ready.single().finalName)
    }

    @Test fun lone_original_and_lone_result_are_independently_renameable() {
        val originals = photo(1, "wrong_001.jpg", originalPath)
        val result = photo(2, "wrong_002.jpg")
        val considered = plan(listOf(originals, result), GalleryPhotoRenameRule.Exact("wrong", "right"))
        assertEquals(2, considered.ready.size)
        assertTrue(considered.ready.all { it.group.photos.size == 1 })
    }

    @Test fun an_ambiguous_or_conflicting_capture_record_is_not_guessed() {
        val result = photo(1, "wrong_001.jpg")
        val original = photo(2, "wrong_001.jpg", originalPath)
        val caseVariant = photo(3, "WRONG_001.jpg", originalPath)
        val withDuplicates = GalleryPhotoRenamePolicy.groups(
            listOf(result), listOf(result, original, caseVariant),
        )
        assertTrue(withDuplicates.single().pairingIssue != null)
        val recordedSingle = GalleryPhotoRenamePolicy.groups(
            listOf(result), listOf(result, original), indexedPairs = mapOf(1L to emptySet()),
        )
        assertEquals(listOf(result), recordedSingle.single().photos)
        val missingRecordPair = GalleryPhotoRenamePolicy.groups(
            listOf(result), listOf(result, original), indexedPairs = mapOf(1L to setOf(9L)),
        )
        assertTrue(missingRecordPair.single().pairingIssue != null)
    }

    @Test fun default_excludes_both_contenders_and_preserves_other_ready_photos() {
        val selected = listOf(photo(1, "Draper_꽃눈_010.jpg"), photo(2, "Draper_줄기_010.jpg"),
            photo(3, "Draper_꽃눈_011.jpg"))
        val considered = plan(selected, GalleryPhotoRenameRule.Position(1, "잎눈"))
        assertEquals(listOf(GalleryRenameStatus.Conflict, GalleryRenameStatus.Conflict, GalleryRenameStatus.Ready),
            considered.rows.map { it.status })
        assertEquals(setOf(1L, 2L), considered.conflictGroup(1))
    }

    @Test fun manual_names_have_priority_then_automatic_uses_the_lowest_common_free_suffix() {
        val first = photo(1, "Draper_꽃눈_010.jpg")
        val second = photo(2, "Draper_줄기_010.jpg")
        val third = photo(3, "Draper_새가지_010.jpg")
        val originals = listOf(photo(4, second.displayName, originalPath), photo(5, third.displayName, originalPath))
        val occupied = photo(6, "Draper_잎눈_010 (2).jpg", originalPath)
        val selected = listOf(first, second, third)
        val considered = plan(selected, GalleryPhotoRenameRule.Position(1, "잎눈"), selected + originals + occupied,
            mapOf(1L to GalleryRenameResolution.Manual("Draper_잎눈"),
                2L to GalleryRenameResolution.Automatic, 3L to GalleryRenameResolution.Automatic))
        assertEquals(listOf("Draper_잎눈_010.jpg", "Draper_잎눈_010 (3).jpg", "Draper_잎눈_010 (4).jpg"),
            considered.ready.map { it.finalName })
    }

    @Test fun only_original_folder_collision_excludes_the_pair_until_explicit_auto_resolution() {
        val result = photo(1, "wrong_001.jpg")
        val original = photo(2, result.displayName, originalPath)
        val occupied = photo(3, "right_001.jpg", originalPath)
        val rule = GalleryPhotoRenameRule.Exact("wrong", "right")
        val all = listOf(result, original, occupied)
        assertEquals(GalleryRenameStatus.Conflict, plan(listOf(result), rule, all).rows.single().status)
        val automatic = plan(listOf(result), rule, all, mapOf(1L to GalleryRenameResolution.Automatic))
        assertEquals("right_001 (2).jpg", automatic.ready.single().finalName)
        assertEquals(2, automatic.writablePhotos.size)
    }

    @Test fun chained_names_remain_occupied_when_a_predecessor_is_excluded() {
        val selected = listOf(photo(1, "AA_001.jpg"), photo(2, "A_001.jpg"), photo(3, "AAA_001.jpg"))
        val blocker = photo(4, "B_001.jpg")
        val considered = plan(selected, GalleryPhotoRenameRule.Literal("unused", ""), selected + blocker,
            mapOf(1L to GalleryRenameResolution.Manual("A"), 2L to GalleryRenameResolution.Manual("B"),
                3L to GalleryRenameResolution.Manual("AA")))
        assertTrue(considered.ready.isEmpty())
        assertEquals(listOf(GalleryRenameStatus.Conflict, GalleryRenameStatus.Conflict, GalleryRenameStatus.Conflict),
            considered.rows.map { it.status })
    }

    @Test fun competing_manual_names_are_rejected_then_revalidated_when_one_switches_to_auto() {
        val selected = listOf(photo(1, "Draper_꽃눈_010.jpg"), photo(2, "Draper_줄기_010.jpg"))
        val manual = GalleryRenameResolution.Manual("Draper_잎눈")
        val rule = GalleryPhotoRenameRule.Position(1, "잎눈")
        assertTrue(plan(selected, rule, resolutions = mapOf(1L to manual, 2L to manual)).ready.isEmpty())
        val resolved = plan(selected, rule,
            resolutions = mapOf(1L to manual, 2L to GalleryRenameResolution.Automatic))
        assertEquals(listOf("Draper_잎눈_010.jpg", "Draper_잎눈_010 (2).jpg"), resolved.ready.map { it.finalName })
    }
}

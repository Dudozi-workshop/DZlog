package com.dudoziworkshop.dzlog.data.project

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.dudoziworkshop.dzlog.domain.project.CaptureProject
import com.dudoziworkshop.dzlog.domain.project.CaptureProjectRegistry
import com.dudoziworkshop.dzlog.domain.project.CaptureReservation
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class LegacyCaptureMigrationTest {
    private lateinit var database: CaptureRecordRoomDatabase
    private lateinit var store: CaptureRecordRoomStore
    private lateinit var migration: LegacyCaptureMigration

    @Before fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context, CaptureRecordRoomDatabase::class.java,
        ).build()
        store = CaptureRecordRoomStore(database)
        migration = LegacyCaptureMigration(store)
    }

    @After fun tearDown() = database.close()

    private fun legacy(
        projectId: String = "A",
        groups: List<List<String>> = listOf(listOf("content://result", "content://original")),
    ): CaptureProjectRegistry = CaptureProjectRegistry(
        projects = listOf(CaptureProject(
            id = projectId, title = "현장", createdAt = 100L,
            captureUriGroups = groups,
        ))
    )

    private fun result(uri: String = "content://result") = LegacyCapturePhoto(
        uri, "Pictures/DZlog/T1/", 300L,
    )

    private fun original(uri: String = "content://original") = LegacyCapturePhoto(
        uri, "Pictures/DZlog/T1/original/", 300L,
    )

    @Test fun originalAndResultPairBecomesOneLogicalRecord() = runBlocking {
        val snapshot = legacy()
        val report = migration.importAvailable(snapshot, listOf(result(), original()))
        assertEquals(1, report.inserted)
        assertTrue(report.unresolved.isEmpty())
        assertEquals(1, store.countForProject("A"))
        val record = store.forProject("A").single()
        assertEquals("content://result", record.resultUri)
        assertEquals("content://original", record.originalUri)
        assertNull(record.templateId) // v1 never recorded this value.
        assertEquals("Pictures/DZlog/T1/original/", record.originalRelativePath)
        assertEquals(1, snapshot.find("A")?.captureUriGroups?.size) // v1 left intact
    }

    @Test fun missingPhotosRemainInLegacyMetadataForLaterRecovery() = runBlocking {
        val snapshot = legacy()
        val report = migration.importAvailable(snapshot, emptyList())
        assertEquals(0, report.inserted)
        assertEquals(LegacyCaptureGap.MISSING_MEDIA, report.unresolved.single().reason)
        assertEquals(0, store.countForProject("A"))
        assertEquals(2, snapshot.find("A")!!.captureUriGroups.single().size)
    }

    @Test fun aMissingOriginalIsImportedWithoutInventingIt() = runBlocking {
        val report = migration.importAvailable(legacy(), listOf(result()))
        assertEquals(1, report.inserted)
        assertEquals(LegacyCaptureGap.PARTIAL_MEDIA, report.unresolved.single().reason)
        assertNull(store.forProject("A").single().originalUri)
    }

    @Test fun stableIdsSurviveOrderAndAvailabilityChanges() {
        val group = legacy(groups = listOf(listOf("content://result", "content://original")))
        val reordered = legacy(groups = listOf(listOf("content://original", "content://result")))
        val full = LegacyCaptureMigrationPolicy.plan(group, listOf(original(), result())).candidates.single()
        val partial = LegacyCaptureMigrationPolicy.plan(group, listOf(result())).candidates.single()
        val otherOrder = LegacyCaptureMigrationPolicy.plan(reordered, listOf(original(), result())).candidates.single()
        assertEquals(full.record.captureId, partial.record.captureId)
        assertEquals(full.record.captureId, otherOrder.record.captureId)
    }

    @Test fun rereadingSameLegacyGroupDoesNotDuplicateRoomRows() = runBlocking {
        val snapshot = legacy()
        val first = migration.importAvailable(snapshot, listOf(original(), result()))
        val second = migration.importAvailable(snapshot, listOf(original(), result()))
        assertEquals(1, first.inserted)
        assertEquals(0, second.inserted)
        assertEquals(1, second.alreadyIndexed)
        assertEquals(1, store.countForProject("A"))
    }

    @Test fun partialRecordIsNotRewrittenWhenMissingMediaReappears() = runBlocking {
        val snapshot = legacy()
        migration.importAvailable(snapshot, listOf(result()))
        val again = migration.importAvailable(snapshot, listOf(result(), original()))
        assertEquals(0, again.inserted)
        assertEquals(1, again.alreadyIndexed)
        assertNull(store.forProject("A").single().originalUri)
    }

    @Test fun existingIndexedReceiptWithSameProjectIsNotDuplicated() = runBlocking {
        val room = requireNotNull(
            CaptureReservation("live-id", "A", "T1", 500L).committed(
                originalUri = "content://original",
                resultUri = "content://result",
            )
        )
        store.appendCommitted(room)
        val report = migration.importAvailable(legacy(), listOf(result(), original()))
        assertEquals(0, report.inserted)
        assertEquals(1, report.alreadyIndexed)
        assertEquals(listOf(room), store.forProject("A"))
    }

    @Test fun aPhotoAlreadyOwnedByAnotherProjectIsNeverReassigned() = runBlocking {
        val room = requireNotNull(
            CaptureReservation("live-id", "B", "T1", 500L).committed(
                resultUri = "content://result",
            )
        )
        store.appendCommitted(room)
        val report = migration.importAvailable(legacy(), listOf(result(), original()))
        assertEquals(0, report.inserted)
        assertEquals(1, report.conflicts.size)
        assertEquals(0, store.countForProject("A"))
        assertEquals(1, store.countForProject("B"))
    }

    @Test fun nonMediaAndAmbiguousGroupsAreNotSilentlyImported() {
        val snapshot = legacy(groups = listOf(
            listOf("content://a", "content://b", "content://c"),
            listOf("content://a", "content://b"),
        ))
        val report = LegacyCaptureMigrationPolicy.plan(snapshot, listOf(
            result("content://a"), result("content://b"), result("content://c")
        ))
        assertEquals(0, report.candidates.size)
        assertEquals(listOf(
            LegacyCaptureGap.TOO_MANY_URIS,
            LegacyCaptureGap.AMBIGUOUS_MEDIA_ROLES,
        ), report.issues.map { it.reason })
    }

    @Test fun laterOldPhotosRemainAccessibleAfterIncrementalImport() = runBlocking {
        val snapshot = legacy(groups = listOf(
            listOf("content://first"),
            listOf("content://later"),
        ))
        val first = migration.importAvailable(snapshot, listOf(result("content://first")))
        assertEquals(1, first.inserted)
        assertEquals(1, first.unresolved.size)
        val second = migration.importAvailable(snapshot, listOf(
            result("content://first"), result("content://later")
        ))
        assertEquals(1, second.inserted)
        assertEquals(1, second.alreadyIndexed)
        assertEquals(2, store.countForProject("A"))
        assertEquals(2, snapshot.find("A")?.captureCount)
    }
}

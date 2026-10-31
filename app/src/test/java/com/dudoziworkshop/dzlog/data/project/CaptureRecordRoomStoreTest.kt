package com.dudoziworkshop.dzlog.data.project

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import android.content.Context
import com.dudoziworkshop.dzlog.domain.project.CaptureReservation
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class CaptureRecordRoomStoreTest {
    private lateinit var database: CaptureRecordRoomDatabase
    private lateinit var store: CaptureRecordRoomStore

    @Before fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context, CaptureRecordRoomDatabase::class.java,
        ).build()
        store = CaptureRecordRoomStore(database)
    }

    @After fun tearDown() {
        database.close()
    }

    private fun record(id: String, project: String? = "A", time: Long = 100L) =
        requireNotNull(CaptureReservation(id, project, "T", time).committed(
            resultUri = "content://photos/$id/result",
            originalUri = "content://photos/$id/original",
            resultRelativePath = "Pictures/DZlog/$id/",
            originalRelativePath = "Pictures/DZlog/$id/original/",
        ))

    @Test fun ordinaryAndProjectRecordsPersistIndependently() = runBlocking {
        val normal = record("normal", project = null)
        val a = record("a", project = "A")
        assertTrue(store.appendCommitted(normal))
        assertTrue(store.appendCommitted(a))
        assertEquals(normal, store.findById("normal"))
        assertEquals(listOf(a), store.forProject("A"))
        assertEquals(1, store.countForProject("A"))
    }

    @Test fun exactReceiptReplayDoesNotDuplicate() = runBlocking {
        val a = record("a")
        assertTrue(store.appendCommitted(a))
        assertFalse(store.appendCommitted(a))
        assertEquals(1, store.countForProject("A"))
    }

    @Test fun conflictingProjectOrMediaUriCannotBeReused() = runBlocking {
        val a = record("a", "A")
        store.appendCommitted(a)
        val conflict = runCatching { store.appendCommitted(a.copy(projectId = "B")) }.exceptionOrNull()
        assertTrue(conflict is IllegalArgumentException)
        val alias = record("b", "B").copy(resultUri = a.originalUri)
        val reused = runCatching { store.appendCommitted(alias) }.exceptionOrNull()
        assertTrue(reused is IllegalStateException)
        assertEquals(1, store.countForProject("A"))
        assertEquals(0, store.countForProject("B"))
    }

    @Test fun latestCaptureAndOrderingUseCaptureTimestamp() = runBlocking {
        val a = record("a", "A", time = 100)
        val b = record("b", "B", time = 300)
        val c = record("c", "A", time = 200)
        store.appendCommitted(a)
        store.appendCommitted(b)
        store.appendCommitted(c)
        assertEquals(listOf(c, a), store.forProject("A"))
        assertEquals(c, store.latestForProject("A"))
    }

    @Test fun partialDeletionLeavesOriginalAndFullDeletionRemovesCapture() = runBlocking {
        val a = record("a")
        store.appendCommitted(a)
        assertEquals(1, store.afterVerifiedDeletion(setOf(requireNotNull(a.resultUri))))
        assertEquals(1, store.countForProject("A"))
        val remaining = requireNotNull(store.findById("a"))
        assertNull(remaining.resultUri)
        assertEquals(a.originalUri, remaining.displayUri)
        assertNull(remaining.resultRelativePath)
        assertEquals(1, store.afterVerifiedDeletion(setOf(requireNotNull(a.originalUri))))
        assertEquals(0, store.countForProject("A"))
        assertNull(store.findById("a"))
    }

    @Test fun unverifiedOrUnrelatedUriCannotEraseExistingCapture() = runBlocking {
        val a = record("a")
        store.appendCommitted(a)
        assertEquals(0, store.afterVerifiedDeletion(emptySet()))
        assertEquals(0, store.afterVerifiedDeletion(setOf("content://other")))
        assertEquals(a, store.findById("a"))
    }

    @Test fun dbReopenKeepsRecords() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "capture-record-test-" + java.util.UUID.randomUUID().toString() + ".db"
        val first = Room.databaseBuilder(
            context, CaptureRecordRoomDatabase::class.java, name,
        ).build()
        try {
            val a = record("durable")
            CaptureRecordRoomStore(first).appendCommitted(a)
        } finally {
            first.close()
        }
        val reopened = Room.databaseBuilder(
            context, CaptureRecordRoomDatabase::class.java, name,
        ).build()
        try {
            assertEquals(record("durable"), CaptureRecordRoomStore(reopened).findById("durable"))
        } finally {
            reopened.close()
            context.deleteDatabase(name)
        }
    }
}

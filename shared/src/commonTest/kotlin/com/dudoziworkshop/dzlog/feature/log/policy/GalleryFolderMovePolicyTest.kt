package com.dudoziworkshop.dzlog.feature.log.policy

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class GalleryFolderMovePolicyTest {
    private val root = GalleryFolderIndexPolicy.ROOT
    private val source = root + "data/"
    private val target = root + "new/"
    private val paths = listOf(root, source, source + "inside/", target, target + "original/", root + "data2/")

    @Test
    fun validatesEveryDestinationAgainstTheDirectoryListing() {
        for (invalid in listOf(source, source + "inside/", root, target + "original/", root + "missing/")) {
            assertFailsWith<IllegalArgumentException> { GalleryFolderMovePolicy.checkedMovedPath(source, invalid, paths) }
        }
        assertFailsWith<IllegalArgumentException> { GalleryFolderMovePolicy.checkedMovedPath(target + "original/", source, paths) }
        assertEquals(root + "data2/data/", GalleryFolderMovePolicy.checkedMovedPath(source, root + "data2/", paths))
    }

    @Test
    fun rejectsFoldersCreatedOrRemovedAfterChoosingTheTarget() {
        assertEquals(target + "data/", GalleryFolderMovePolicy.checkedMovedPath(source, target, paths))
        for (changed in listOf(paths + (target + "DATA/"), paths - source, paths - target)) {
            assertFailsWith<IllegalArgumentException> { GalleryFolderMovePolicy.checkedMovedPath(source, target, changed) }
        }
    }
}

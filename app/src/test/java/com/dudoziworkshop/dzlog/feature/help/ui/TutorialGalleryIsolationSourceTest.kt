package com.dudoziworkshop.dzlog.feature.help.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Prevent tutorial gallery UI from accidentally returning to actual user photos. */
class TutorialGalleryIsolationSourceTest {
    @Test fun galleryTutorialNeverAccessesLiveGalleryOrMediaStore() {
        val src = File("src/main/java/com/dudoziworkshop/dzlog/feature/help/ui/LiveTutorialScreens.kt").readText()
        assertTrue(src.contains("\"gallery\" -> TutorialGallerySampleScreen(location)"))
        assertFalse(src.contains("DzlogMediaStoreReader"))
        assertFalse(src.contains("LogFolderScreen("))
        assertFalse(src.contains("LogViewerScreen("))
    }

    @Test fun gallerySamplesUseBundledDrawablesNotPlaceholders() {
        val src = File("src/main/java/com/dudoziworkshop/dzlog/feature/help/ui/TutorialGallerySample.kt").readText()
        assertTrue(src.contains("R.drawable.onboarding_gallery_photo"))
        assertFalse(src.contains("원본 사진 준비 중"))
    }
}

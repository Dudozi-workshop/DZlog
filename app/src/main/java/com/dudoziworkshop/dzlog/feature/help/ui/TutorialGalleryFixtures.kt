package com.dudoziworkshop.dzlog.feature.help.ui

import android.content.Context
import android.net.Uri
import com.dudoziworkshop.dzlog.R
import com.dudoziworkshop.dzlog.domain.model.MediaImageItem
import com.dudoziworkshop.dzlog.feature.log.policy.GalleryFolderIndexPolicy

/**
 * Bundled tutorial photos with virtual storage paths.
 * No filesystem, MediaStore, or user folder writes are performed.
 */
internal object TutorialGalleryFixtures {
    private const val ROOT = GalleryFolderIndexPolicy.ROOT
    const val DOG = ROOT + "단이 사진/"
    const val WALK = DOG + "산책/"
    const val WORK = ROOT + "업무 기록/"
    const val TRAVEL = ROOT + "여행 기록/"
    val folderPaths = listOf(DOG, WALK, WORK, TRAVEL)

    fun photos(context: Context): List<MediaImageItem> {
        val pictures = listOf(
            R.drawable.onboarding_gallery_photo,
            R.drawable.onboarding_hero_photo,
            R.drawable.onboarding_starter_photo,
        )
        val paths = listOf(ROOT, DOG, DOG, WALK, WORK, TRAVEL, DOG + "original/")
        return paths.mapIndexed { index, path ->
            MediaImageItem(
                id = -100L - index,
                uri = Uri.parse("android.resource://" + context.packageName + "/" + pictures[index % pictures.size]),
                // Exact matching names permit the real gallery pairing policy to find the original.
                displayName = if (path == DOG + "original/") "DZlog_안내_2.jpg"
                    else "DZlog_안내_" + (index + 1) + ".jpg",
                relativePath = path,
                dateAddedSeconds = 1770000000L - index * 120L,
            )
        }
    }
}

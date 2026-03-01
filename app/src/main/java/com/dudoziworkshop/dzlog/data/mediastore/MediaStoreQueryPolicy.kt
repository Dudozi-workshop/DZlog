package com.dudoziworkshop.dzlog.data.mediastore

import android.os.Build
import android.provider.MediaStore

internal object MediaStoreQueryPolicy {

    internal data class WhereClause(
        val selection: String,
        val selectionArgs: Array<String>
    )

    internal fun normalizeRelativePath(relativePath: String): String {
        val p = relativePath
            .trim()
            .trimStart('/')
            .replace("\\", "/")
        return if (p.endsWith("/")) p else "$p/"
    }

    internal fun normalizeRelativePathVariants(relativePath: String): Pair<String, String> {
        val withSlash = normalizeRelativePath(relativePath)
        val withoutSlash = withSlash.removeSuffix("/")
        return withSlash to withoutSlash
    }

    internal fun whereExactRelativePath(
        relativePath: String,
        includeTrashed: Boolean = false
    ): WhereClause {
        val (withSlash, withoutSlash) = normalizeRelativePathVariants(relativePath)
        return WhereClause(
            selection = exactRelativePathBaseSelection() + if (includeTrashed) "" else notTrashedClause(),
            selectionArgs = arrayOf(withSlash, withoutSlash)
        )
    }

    internal fun whereRelativePathLike(
        likePattern: String,
        includeTrashed: Boolean = false
    ): WhereClause {
        return WhereClause(
            selection = relativePathLikeBaseSelection() + if (includeTrashed) "" else notTrashedClause(),
            selectionArgs = arrayOf(likePattern)
        )
    }

    private fun exactRelativePathBaseSelection(): String {
        return "(${MediaStore.Images.Media.RELATIVE_PATH} = ? OR ${MediaStore.Images.Media.RELATIVE_PATH} = ?)"
    }

    private fun relativePathLikeBaseSelection(): String {
        return "${MediaStore.Images.Media.RELATIVE_PATH} LIKE ?"
    }

    private fun notTrashedClause(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            " AND (${MediaStore.Images.Media.IS_TRASHED} = 0 OR ${MediaStore.Images.Media.IS_TRASHED} IS NULL)"
        } else {
            ""
        }
    }
}

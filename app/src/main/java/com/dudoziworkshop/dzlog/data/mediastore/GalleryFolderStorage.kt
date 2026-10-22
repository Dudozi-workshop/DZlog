package com.dudoziworkshop.dzlog.data.mediastore

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import com.dudoziworkshop.dzlog.feature.log.policy.GalleryFolderIndexPolicy
import com.dudoziworkshop.dzlog.feature.log.policy.GalleryFolderNamePolicy

/**
 * Required SAF integration for physical DZlog directory management.
 * MediaStore continues to own photo indexing; never create dummy media files.
 */
class GalleryFolderStorage(context: Context) {
    private val resolver = context.contentResolver
    private val preferences = context.getSharedPreferences("dzlog_gallery_folder_tree", Context.MODE_PRIVATE)

    private data class Entry(val id: String, val name: String, val mimeType: String)

    fun isConnected(): Boolean = connectedTree() != null

    /**
     * The onboarding picker may grant Pictures instead of a not-yet-created DZlog folder.
     * Only onboarding may provision the child directory; all storage operations remain
     * constrained to Pictures/DZlog, even when the persisted SAF grant is broader.
     */
    fun connect(treeUri: Uri, allowPicturesForOnboarding: Boolean = false) {
        val documentId = runCatching {
            DocumentsContract.getTreeDocumentId(treeUri).trimEnd('/')
        }.getOrNull()
        val exactDzlog = documentId == DZLOG_DOCUMENT_ID
        val onboardingPictures = allowPicturesForOnboarding && documentId == PICTURES_DOCUMENT_ID
        require(
            treeUri.authority == EXTERNAL_STORAGE_AUTHORITY && (exactDzlog || onboardingPictures)
        ) {
            if (allowPicturesForOnboarding) {
                "내장 저장공간의 Pictures 또는 Pictures/DZlog 폴더를 선택해 주세요."
            } else {
                "내장 저장공간의 Pictures/DZlog 폴더를 정확히 선택해 주세요."
            }
        }
        if (onboardingPictures) ensureDzlogDirectory(treeUri)
        require(isExpectedRootAccessible(treeUri)) {
            "Pictures/DZlog 폴더를 확인할 수 없습니다. 다시 시도해 주세요."
        }
        resolver.takePersistableUriPermission(
            treeUri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
        )
        preferences.edit().putString(TREE_KEY, treeUri.toString()).apply()
    }

    /** Never create media placeholders or overwrite an existing file or directory. */
    private fun ensureDzlogDirectory(picturesTree: Uri) {
        require(isAccessibleDirectory(picturesTree, PICTURES_DOCUMENT_ID, "Pictures")) {
            "선택한 Pictures 폴더를 확인할 수 없습니다."
        }
        val picturesUri = DocumentsContract.buildDocumentUriUsingTree(
            picturesTree, PICTURES_DOCUMENT_ID,
        )
        val sameName = listChildren(picturesTree, picturesUri).filter {
            it.name.equals("DZlog", ignoreCase = true)
        }
        require(sameName.size <= 1 &&
            sameName.all {
                it.name == "DZlog" &&
                    it.id == DZLOG_DOCUMENT_ID &&
                    it.mimeType == DocumentsContract.Document.MIME_TYPE_DIR
            }
        ) { "Pictures 안에 이름이 겹치는 항목이 있어 DZlog 폴더를 만들 수 없습니다." }
        if (sameName.isEmpty()) {
            val created = DocumentsContract.createDocument(
                resolver, picturesUri, DocumentsContract.Document.MIME_TYPE_DIR, "DZlog",
            ) ?: error("DZlog 폴더를 자동으로 만들지 못했습니다. 저장소 설정을 확인해 주세요.")
            check(DocumentsContract.getDocumentId(created) == DZLOG_DOCUMENT_ID) {
                "생성된 폴더 경로가 예상과 다릅니다. 파일 관리자에서 확인해 주세요."
            }
        }
    }

    /**
     * Enumerate real immediate child directories, including ones with no photos.
     * When the user has not connected the folder this returns an empty list;
     * MediaStore-backed photo folders remain visible independently.
     */
    fun listImmediateFolderPaths(relativePath: String): List<String> {
        val tree = connectedTree() ?: return emptyList()
        val parent = resolveDirectory(tree, relativePath) ?: return emptyList()
        return listChildren(tree, parent)
            .asSequence()
            .filter { it.mimeType == DocumentsContract.Document.MIME_TYPE_DIR }
            .map { relativePath.trimEnd('/') + "/" + it.name + "/" }
            .toList()
    }

    /** Folder destinations only; never includes files or the source subtree. */
    fun listDestinationFolders(sourcePath: String): List<String> {
        val tree = connectedTree() ?: return listOf(GalleryFolderIndexPolicy.ROOT)
        val root = GalleryFolderIndexPolicy.ROOT
        val result = mutableListOf(root)
        val queue = ArrayDeque<Pair<String, Uri>>()
        queue.add(root to DocumentsContract.buildDocumentUriUsingTree(
            tree, DZLOG_DOCUMENT_ID,
        ))
        while (queue.isNotEmpty()) {
            val (path, parent) = queue.removeFirst()
            if (result.size >= 300) break
            listChildren(tree, parent).filter {
                it.mimeType == DocumentsContract.Document.MIME_TYPE_DIR &&
                    !it.name.equals("original", true)
            }.forEach { entry ->
                val childPath = path + entry.name + "/"
                if (childPath != sourcePath && !childPath.startsWith(sourcePath)) {
                    result += childPath
                    queue.add(childPath to DocumentsContract.buildDocumentUriUsingTree(tree, entry.id))
                }
            }
        }
        return result.filter { it != GalleryFolderIndexPolicy.parentOf(sourcePath) }
    }

    fun createFolder(relativePath: String, rawName: String): String {
        val name = GalleryFolderNamePolicy.normalize(rawName)
        val tree = connectedTree()
            ?: error("먼저 Pictures/DZlog 폴더 접근을 연결해 주세요.")
        val parent = resolveDirectory(tree, relativePath)
            ?: error("상위 폴더가 저장소에 존재하지 않습니다. 폴더 연결을 확인해 주세요.")
        require(listChildren(tree, parent).none { it.name.equals(name, ignoreCase = true) }) {
            "같은 이름의 파일 또는 폴더가 이미 존재합니다."
        }
        val created = DocumentsContract.createDocument(
            resolver, parent, DocumentsContract.Document.MIME_TYPE_DIR, name,
        ) ?: error("폴더 생성에 실패했습니다.")
        // SAF providers may silently change a name. Report that rather than claim success.
        val actual = resolver.query(
            created,
            arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME),
            null, null, null,
        )?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
        check(actual == name) {
            "저장소에서 생성된 폴더 이름을 확인할 수 없습니다. 파일 관리자에서 확인해 주세요."
        }
        return relativePath.trimEnd('/') + "/" + name + "/"
    }

    /** Renames a directory in-place without touching camera template preferences. */
    fun renameFolder(sourcePath: String, newName: String): String {
        val target = com.dudoziworkshop.dzlog.feature.log.policy.GalleryFolderOperationPolicy
            .renamedPath(sourcePath, newName)
        if (target == sourcePath) return sourcePath
        val tree = connectedTree() ?: error("먼저 DZlog 폴더 접근을 연결해 주세요.")
        val source = resolveDirectory(tree, sourcePath) ?: error("원본 폴더를 찾지 못했습니다.")
        val parentPath = requireNotNull(GalleryFolderIndexPolicy.parentOf(sourcePath))
        val parent = resolveDirectory(tree, parentPath) ?: error("상위 폴더를 찾지 못했습니다.")
        val requested = target.trimEnd('/').substringAfterLast('/')
        require(listChildren(tree, parent).none { it.name.equals(requested, true) }) {
            "동일한 이름의 폴더가 이미 존재합니다."
        }
        val renamed = DocumentsContract.renameDocument(resolver, source, requested)
            ?: error("폴더 이름 변경을 완료하지 못했습니다.")
        require(queryName(renamed) == requested) { "변경된 폴더 이름을 확인하지 못했습니다." }
        return target
    }

    /** Moves the whole directory, including originals and nested media. */
    fun moveFolder(sourcePath: String, destinationParentPath: String): String {
        val target = com.dudoziworkshop.dzlog.feature.log.policy.GalleryFolderOperationPolicy
            .movedPath(sourcePath, destinationParentPath)
        val tree = connectedTree() ?: error("먼저 DZlog 폴더 접근을 연결해 주세요.")
        val source = resolveDirectory(tree, sourcePath) ?: error("원본 폴더를 찾지 못했습니다.")
        val oldParentPath = requireNotNull(GalleryFolderIndexPolicy.parentOf(sourcePath))
        val oldParent = resolveDirectory(tree, oldParentPath) ?: error("기존 상위 폴더를 찾지 못했습니다.")
        val destination = resolveDirectory(tree, destinationParentPath)
            ?: error("이동할 대상 폴더가 존재하지 않습니다.")
        val name = sourcePath.trimEnd('/').substringAfterLast('/')
        require(listChildren(tree, destination).none { it.name.equals(name, true) }) {
            "대상 위치에 동일한 이름이 이미 존재합니다."
        }
        val moved = DocumentsContract.moveDocument(resolver, source, oldParent, destination)
            ?: error("이 저장소에서는 폴더 이동을 지원하지 않습니다.")
        require(queryName(moved) == name) { "이동한 폴더 이름을 확인하지 못했습니다." }
        return target
    }

    /** Real files of every type, including original/ and empty nested directories. */
    fun prepareDeletion(path: String): com.dudoziworkshop.dzlog.feature.log.policy.GalleryFolderDeletePlan {
        val policy = com.dudoziworkshop.dzlog.feature.log.policy.GalleryFolderDeletePolicy
        val source = policy.source(path)
        val tree = connectedTree() ?: error("먼저 DZlog 폴더 접근을 연결해 주세요.")
        val root = resolveDirectory(tree, source) ?: return policy.plan(source, emptyList())
        val entries = mutableListOf<com.dudoziworkshop.dzlog.feature.log.policy.GalleryFolderDeleteEntry>()
        entries += com.dudoziworkshop.dzlog.feature.log.policy.GalleryFolderDeleteEntry(
            DocumentsContract.getDocumentId(root), source, DocumentsContract.Document.MIME_TYPE_DIR,
        )
        val queue = ArrayDeque<Pair<String, Uri>>()
        queue.add(source to root)
        val visited = mutableSetOf(DocumentsContract.getDocumentId(root))
        while (queue.isNotEmpty()) {
            val (parentPath, parentUri) = queue.removeFirst()
            listChildren(tree, parentUri).forEach { child ->
                require(child.name.isNotBlank() && '/' !in child.name && child.name != "." && child.name != "..") {
                    "저장소 파일 이름을 확인하지 못했습니다."
                }
                check(visited.add(child.id)) { "중복된 저장소 항목을 확인했습니다." }
                val directory = child.mimeType == DocumentsContract.Document.MIME_TYPE_DIR
                val childPath = parentPath + child.name + if (directory) "/" else ""
                entries += com.dudoziworkshop.dzlog.feature.log.policy.GalleryFolderDeleteEntry(child.id, childPath, child.mimeType)
                check(entries.size <= 5000) { "폴더 내용이 너무 많습니다. 하위 폴더를 나누어 삭제해 주세요." }
                if (directory) queue.add(childPath to DocumentsContract.buildDocumentUriUsingTree(tree, child.id))
            }
        }
        return policy.plan(source, entries)
    }

    /** Delete individually, then only empty directories. New or failed files prevent parent removal. */
    fun deleteFolder(plan: com.dudoziworkshop.dzlog.feature.log.policy.GalleryFolderDeletePlan):
        com.dudoziworkshop.dzlog.feature.log.policy.GalleryFolderDeletePlan {
        val current = prepareDeletion(plan.source)
        com.dudoziworkshop.dzlog.feature.log.policy.GalleryFolderDeletePolicy.requireUnchanged(plan, current)
        val tree = connectedTree() ?: error("폴더 접근 권한을 확인해 주세요.")
        val ordered = plan.entries.filterNot { it.directory } +
            plan.entries.filter { it.directory }.sortedByDescending { it.path.count { char -> char == '/' } }
        ordered.forEach { entry ->
            runCatching {
                val parentPath = entry.path.trimEnd('/').substringBeforeLast('/') + "/"
                val parent = resolveDirectory(tree, parentPath) ?: return@runCatching
                val name = entry.path.trimEnd('/').substringAfterLast('/')
                val matching = listChildren(tree, parent).singleOrNull { it.id == entry.documentId && it.name == name && it.mimeType == entry.mimeType }
                    ?: return@runCatching
                val uri = DocumentsContract.buildDocumentUriUsingTree(tree, matching.id)
                if (!entry.directory || listChildren(tree, uri).isEmpty()) {
                    DocumentsContract.deleteDocument(resolver, uri)
                }
            }
        }
        // A failed verification throws; an unreadable directory must never be reported as deleted.
        return prepareDeletion(plan.source)
    }

    private fun queryName(uri: Uri): String? = resolver.query(
        uri, arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME),
        null, null, null,
    )?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }

    private fun connectedTree(): Uri? {
        val encoded = preferences.getString(TREE_KEY, null) ?: return null
        val tree = runCatching { Uri.parse(encoded) }.getOrNull() ?: return null
        return tree.takeIf { uri ->
            resolver.persistedUriPermissions.any { permission ->
                permission.uri == uri && permission.isReadPermission && permission.isWritePermission
            } && isExpectedRootAccessible(uri)
        }
    }

    private fun isExpectedRootAccessible(tree: Uri): Boolean {
        val treeId = runCatching {
            DocumentsContract.getTreeDocumentId(tree).trimEnd('/')
        }.getOrNull() ?: return false
        if (tree.authority != EXTERNAL_STORAGE_AUTHORITY ||
            (treeId != DZLOG_DOCUMENT_ID && treeId != PICTURES_DOCUMENT_ID)
        ) return false
        return isAccessibleDirectory(tree, DZLOG_DOCUMENT_ID, "DZlog")
    }

    private fun isAccessibleDirectory(tree: Uri, documentId: String, expectedName: String): Boolean {
        val document = runCatching {
            DocumentsContract.buildDocumentUriUsingTree(tree, documentId)
        }.getOrNull() ?: return false
        return runCatching {
            resolver.query(
                document,
                arrayOf(
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_MIME_TYPE,
                ),
                null, null, null,
            )?.use { cursor ->
                cursor.moveToFirst() &&
                    cursor.getString(0)?.trimEnd('/') == documentId &&
                    cursor.getString(1) == expectedName &&
                    cursor.getString(2) == DocumentsContract.Document.MIME_TYPE_DIR
            } == true
        }.getOrDefault(false)
    }

    private fun resolveDirectory(tree: Uri, path: String): Uri? {
        val root = GalleryFolderIndexPolicy.ROOT
        require(path == root || path.startsWith(root)) { "DZlog 폴더 밖의 경로는 사용할 수 없습니다." }
        var current = DocumentsContract.buildDocumentUriUsingTree(
            tree, DZLOG_DOCUMENT_ID,
        )
        val segments = path.removePrefix(root).trim('/').split('/').filter(String::isNotBlank)
        for (segment in segments) {
            val next = listChildren(tree, current).firstOrNull {
                it.mimeType == DocumentsContract.Document.MIME_TYPE_DIR && it.name == segment
            } ?: return null
            current = DocumentsContract.buildDocumentUriUsingTree(tree, next.id)
        }
        return current
    }

    private fun listChildren(tree: Uri, parent: Uri): List<Entry> {
        val childUri = DocumentsContract.buildChildDocumentsUriUsingTree(
            tree, DocumentsContract.getDocumentId(parent),
        )
        val result = mutableListOf<Entry>()
        resolver.query(
            childUri,
            arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
            ),
            null, null, null,
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getString(0) ?: continue
                val name = cursor.getString(1) ?: continue
                val mime = cursor.getString(2).orEmpty()
                result.add(Entry(id, name, mime))
            }
        } ?: error("폴더 내용을 읽지 못했습니다. 저장소 연결 상태를 확인해 주세요.")
        return result
    }

    companion object {
        private const val TREE_KEY = "connected_dzlog_tree_uri"
        private const val EXTERNAL_STORAGE_AUTHORITY = "com.android.externalstorage.documents"
        private const val PICTURES_DOCUMENT_ID = "primary:Pictures"
        private const val DZLOG_DOCUMENT_ID = "primary:Pictures/DZlog"
    }
}

package com.dudoziworkshop.dzlog.data.mediastore

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import com.dudoziworkshop.dzlog.feature.log.policy.GalleryFolderIndexPolicy
import com.dudoziworkshop.dzlog.feature.log.policy.GalleryFolderNamePolicy

/**
 * Physical empty folders live in the user-granted Pictures/DZlog SAF tree.
 * MediaStore still owns photo indexing; do not create dummy media files.
 */
class GalleryFolderStorage(context: Context) {
    private val resolver = context.contentResolver
    private val preferences = context.getSharedPreferences("dzlog_gallery_folder_tree", Context.MODE_PRIVATE)

    private data class Entry(val id: String, val name: String, val mimeType: String)

    fun isConnected(): Boolean = connectedTree() != null

    fun connect(treeUri: Uri) {
        val documentId = runCatching { DocumentsContract.getTreeDocumentId(treeUri) }.getOrNull()
        require(
            treeUri.authority == "com.android.externalstorage.documents" &&
                documentId?.trimEnd('/') == "primary:Pictures/DZlog"
        ) { "내장 저장공간의 Pictures/DZlog 폴더를 정확히 선택해 주세요." }
        resolver.takePersistableUriPermission(
            treeUri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
        )
        preferences.edit().putString(TREE_KEY, treeUri.toString()).apply()
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

    private fun connectedTree(): Uri? {
        val encoded = preferences.getString(TREE_KEY, null) ?: return null
        val tree = runCatching { Uri.parse(encoded) }.getOrNull() ?: return null
        return tree.takeIf { uri ->
            resolver.persistedUriPermissions.any { permission ->
                permission.uri == uri && permission.isReadPermission && permission.isWritePermission
            }
        }
    }

    private fun resolveDirectory(tree: Uri, path: String): Uri? {
        val root = GalleryFolderIndexPolicy.ROOT
        require(path == root || path.startsWith(root)) { "DZlog 폴더 밖의 경로는 사용할 수 없습니다." }
        var current = DocumentsContract.buildDocumentUriUsingTree(
            tree, DocumentsContract.getTreeDocumentId(tree),
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
    }
}

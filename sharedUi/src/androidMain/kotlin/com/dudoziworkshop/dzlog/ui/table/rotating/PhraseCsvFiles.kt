package com.dudoziworkshop.dzlog.ui.table.rotating

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal actual fun rememberPhraseCsvFiles(
    onRead: (String) -> Unit,
    onFinished: (String?) -> Unit,
): PhraseCsvFiles {
    val resolver = LocalContext.current.contentResolver
    val scope = rememberCoroutineScope()
    val received by rememberUpdatedState(onRead)
    val finished by rememberUpdatedState(onFinished)
    var exportText by remember { mutableStateOf<String?>(null) }
    val reader = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) finished(null) else scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    requireNotNull(resolver.openInputStream(uri)).use { stream ->
                        val bytes = ByteArray(1_000_001)
                        var size = 0
                        while (size < bytes.size) {
                            val count = stream.read(bytes, size, bytes.size - size)
                            if (count < 0) break
                            check(count > 0)
                            size += count
                        }
                        require(size < bytes.size) { "파일 크기가 1 MB를 초과합니다." }
                        bytes.decodeToString(0, size, throwOnInvalidSequence = true)
                    }
                }
            }
            result.fold(received) { finished(it.message ?: "CSV 파일을 읽을 수 없습니다.") }
        }
    }
    val writer = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        val csv = exportText
        exportText = null
        if (uri == null) finished(null) else scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val bytes = requireNotNull(csv).encodeToByteArray()
                    requireNotNull(resolver.openOutputStream(uri, "wt")).use { it.write(bytes) }
                }
            }
            finished(if (result.isSuccess) "CSV 내보내기가 완료되었습니다." else "CSV 저장에 실패했습니다.")
        }
    }
    return PhraseCsvFiles(
        open = { runCatching { reader.launch(arrayOf("*/*")) }.onFailure { finished("파일 선택기를 열 수 없습니다.") } },
        export = { name, csv ->
            exportText = csv
            runCatching { writer.launch(name) }.onFailure { exportText = null; finished("파일 선택기를 열 수 없습니다.") }
        },
    )
}

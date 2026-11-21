package com.dudoziworkshop.dzlog.ui.table.rotating

import androidx.compose.runtime.*
import com.dudoziworkshop.dzlog.domain.model.newTableElementId
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue

@Composable
internal actual fun rememberPhraseCsvFiles(
    onRead: (String) -> Unit,
    onFinished: (String?) -> Unit,
): PhraseCsvFiles {
    val received by rememberUpdatedState(onRead)
    val finished by rememberUpdatedState(onFinished)
    val center = NSNotificationCenter.defaultCenter
    val replyName = remember { "dzlog.phraseCsvReply." + newTableElementId() }
    DisposableEffect(replyName) {
        val observer = center.addObserverForName(replyName, null, NSOperationQueue.mainQueue) { note ->
            val result = note?.`object` as? Map<*, *>
            val csv = result?.get("csv") as? String
            if (csv != null) received(csv) else finished(result?.get("message") as? String)
        }
        onDispose { center.removeObserver(observer) }
    }
    return remember(replyName) {
        PhraseCsvFiles(
            open = { center.postNotificationName("dzlog.phraseCsv", mapOf("reply" to replyName)) },
            export = { name, csv ->
                center.postNotificationName("dzlog.phraseCsv", mapOf("reply" to replyName, "name" to name, "csv" to csv))
            },
        )
    }
}

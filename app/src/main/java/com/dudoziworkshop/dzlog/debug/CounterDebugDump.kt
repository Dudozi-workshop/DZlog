package com.dudoziworkshop.dzlog.debug

import android.content.Context
import android.content.pm.ApplicationInfo
import android.util.Log
import com.dudoziworkshop.dzlog.data.datastore.AppSettings
import com.dudoziworkshop.dzlog.domain.capturepolicy.CaptureCounterPolicy
import com.dudoziworkshop.dzlog.domain.counter.CaptureScopedCounterStream
import com.dudoziworkshop.dzlog.domain.model.SaveMode

object CounterDebugDump {

    internal suspend fun dump(
        tag: String,
        context: Context,
        scopedStream: CaptureScopedCounterStream,
        appSettings: AppSettings,
        nextSeed: Int,
        note: String? = null
    ) {
        dumpLite(
            tag = tag,
            context = context,
            scopedStream = scopedStream,
            saveMode = appSettings.saveMode,
            includePathInCounterScope = appSettings.includePathInCounterScope,
            includeFilenameInCounterScope = appSettings.includeFilenameInCounterScope,
            nextSeed = nextSeed,
            note = note,
        )
    }

    internal suspend fun dumpLite(
        tag: String,
        context: Context,
        scopedStream: CaptureScopedCounterStream,
        saveMode: SaveMode,
        includePathInCounterScope: Boolean,
        includeFilenameInCounterScope: Boolean,
        nextSeed: Int,
        note: String? = null
    ) {
        val isDebuggable = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        if (!isDebuggable) return

        val manualOverrideActive = CaptureCounterPolicy.isManualOverrideActive(
            context = context,
            scopedStream = scopedStream,
        )

        val summaryLine = "[CounterDump][$tag] next=$nextSeed manual=$manualOverrideActive " +
            "saveMode=$saveMode incPath=$includePathInCounterScope incFn=$includeFilenameInCounterScope " +
            "scopeKey=${scopedStream.scopeParts.scopeKey}"
        val pathLine = "[CounterDump][$tag] relPathKey=${scopedStream.captureStreamKey.relativePathKey} " +
            "prefix=${scopedStream.captureStreamKey.prefix}"
        val noteLine = "[CounterDump][$tag] note=${note ?: "-"}"

        Log.d("CounterDump", summaryLine)
        Log.d("CounterDump", pathLine)
        Log.d("CounterDump", noteLine)
    }
}

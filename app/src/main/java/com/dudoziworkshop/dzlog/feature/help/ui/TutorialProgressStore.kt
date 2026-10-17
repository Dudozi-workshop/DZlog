package com.dudoziworkshop.dzlog.feature.help.ui

import android.content.Context
import com.dudoziworkshop.dzlog.feature.help.model.*

/** Separate guide-only preferences: never writes camera settings, templates or photos. */
internal class TutorialProgressStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("dzlog_tutorial_v2", Context.MODE_PRIVATE)
    private fun key(area: String, session: String) = "$area/$session"
    fun page(area: String, session: String) = prefs.getString("page/" + key(area, session), "").orEmpty()
    fun status(area: String, session: String): String = when {
        prefs.getBoolean("done/" + key(area, session), false) -> "완료"
        page(area, session).isNotEmpty() -> "보는 중"
        else -> "미시작"
    }
    fun save(location: TutorialLocation) {
        prefs.edit().putString("page/" + key(location.areaId, location.sessionId), location.pageId).apply()
    }
    fun complete(location: TutorialLocation) {
        prefs.edit().putBoolean("done/" + key(location.areaId, location.sessionId), true).apply()
    }
}

package com.dudoziworkshop.dzlog.feature.help.ui

import android.content.Context
import com.dudoziworkshop.dzlog.feature.help.model.*

/** Separate guide-only preferences: never writes camera settings, templates or photos. */
internal class TutorialProgressStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("dzlog_tutorial_v2", Context.MODE_PRIVATE)
    private fun key(area: String, session: String) = "$area/$session"
    fun page(area: String, session: String): String {
        val saved = prefs.getString("page/" + key(area, session), "").orEmpty()
        return if (area == "camera") cameraGuideSavedPage(session, saved) else saved
    }
    fun status(area: String, session: String): String = when {
        prefs.getBoolean("done/" + key(area, session), false) -> "완료"
        page(area, session).isNotEmpty() -> "보는 중"
        else -> "미시작"
    }
    fun save(location: TutorialLocation) {
        prefs.edit().putString("page/" + key(location.areaId, location.sessionId), location.pageId)
            .putString("last-session/" + location.areaId, location.sessionId).apply()
    }
    fun resume(area: TutorialArea): TutorialLocation {
        val saved = prefs.getString("last-session/" + area.id, null)
        val session = area.sessions.firstOrNull { it.id == saved }
            ?: area.sessions.lastOrNull { page(area.id, it.id).isNotEmpty() }
            ?: area.sessions.first()
        return TutorialLocation(area.id, session.id, session.guidePages()[tutorialPageIndex(session.guidePages(), page(area.id, session.id))].id)
    }
    fun start(area: TutorialArea) = save(TutorialLocation(area.id, area.sessions.first().id, area.sessions.first().guidePages().first().id))
    fun viewed(location: TutorialLocation): Boolean =
        prefs.getBoolean("seen/" + key(location.areaId, location.sessionId) + "/" + location.pageId, false) ||
            status(location.areaId, location.sessionId) == "완료"
    fun markViewed(location: TutorialLocation) {
        prefs.edit().putBoolean("seen/" + key(location.areaId, location.sessionId) + "/" + location.pageId, true).apply()
    }
    fun complete(location: TutorialLocation) {
        prefs.edit().putBoolean("done/" + key(location.areaId, location.sessionId), true).apply()
    }
}

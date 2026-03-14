package com.dudoziworkshop.dzlog

import android.app.Application
import com.google.android.gms.ads.MobileAds

class DzlogApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        val appId = getString(R.string.admob_app_id)
        // Placeholder/예시 App ID 상태에서는 초기화를 건너뛰어 시작 크래시를 방지한다.
        if (isRealAdmobAppId(appId)) {
            MobileAds.initialize(this)
        }
    }

    private fun isRealAdmobAppId(appId: String): Boolean {
        val trimmed = appId.trim()
        if (trimmed.isEmpty()) return false
        if (!trimmed.startsWith("ca-app-pub-")) return false
        if (trimmed.contains('x', ignoreCase = true) || trimmed.contains('y', ignoreCase = true)) return false

        val appIdPattern = Regex("^ca-app-pub-\\d{16}~\\d{10}$")
        return appIdPattern.matches(trimmed)
    }
}

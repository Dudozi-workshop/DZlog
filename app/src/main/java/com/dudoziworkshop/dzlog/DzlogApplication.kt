package com.dudoziworkshop.dzlog

import android.app.Application
import com.google.android.gms.ads.MobileAds

class DzlogApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // AdMob SDK는 앱 시작 시 1회만 초기화한다. (실제 광고 로드/표시는 이후 단계에서 구현)
        MobileAds.initialize(this)
    }
}

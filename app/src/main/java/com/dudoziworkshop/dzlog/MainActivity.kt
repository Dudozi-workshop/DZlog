package com.dudoziworkshop.dzlog

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import com.dudoziworkshop.dzlog.feature.table.policy.BillingEntitlementRuntime
import androidx.lifecycle.lifecycleScope
import com.dudoziworkshop.dzlog.ui.camera.VolumeKeyInputBus
import com.dudoziworkshop.dzlog.ui.onboarding.OnboardingEntryGate
import com.dudoziworkshop.dzlog.ui.theme.DDZTheme

class MainActivity : ComponentActivity() {

    override fun onResume() {
        super.onResume()
        // A cached signed proof is used immediately; Play re-verification happens
        // when the prepared paid runtime is active.
        lifecycleScope.launchWhenResumed { BillingEntitlementRuntime.refresh() }
    }

    override fun onDestroy() {
        BillingEntitlementRuntime.stop()
        super.onDestroy()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (VolumeKeyInputBus.handleKeyEvent(event)) return true
        return super.onKeyDown(keyCode, event)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        BillingEntitlementRuntime.start(applicationContext, lifecycleScope)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            DDZTheme {
                OnboardingEntryGate()
            }
        }
    }
}

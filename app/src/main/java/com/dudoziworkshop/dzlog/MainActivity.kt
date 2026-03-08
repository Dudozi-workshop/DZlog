package com.dudoziworkshop.dzlog

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.dudoziworkshop.dzlog.ui.camera.VolumeKeyInputBus
import com.dudoziworkshop.dzlog.ui.navigation.AppRoot
import com.dudoziworkshop.dzlog.ui.theme.DDZTheme

class MainActivity : ComponentActivity() {

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (VolumeKeyInputBus.handleKeyEvent(event)) return true
        return super.dispatchKeyEvent(event)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DDZTheme {
                AppRoot()
            }
        }
    }
}

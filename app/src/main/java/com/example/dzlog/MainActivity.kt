package com.example.dzlog

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.dzlog.ui.navigation.AppRoot
import com.example.dzlog.ui.theme.DZlogTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DZlogTheme {
                AppRoot()
            }
        }
    }
}

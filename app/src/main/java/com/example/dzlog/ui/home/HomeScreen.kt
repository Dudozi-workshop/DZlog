package com.example.dzlog.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HomeScreen(
    onOpenSettings: () -> Unit,
    onStartCamera: () -> Unit,
    onOpenTableEditor: () -> Unit,
    onOpenLog: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0C0D)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(0.86f)
        ) {
            Text("DZlog", fontSize = 24.sp, color = Color.White)
            Spacer(Modifier.height(20.dp))

            Button(onClick = onStartCamera, modifier = Modifier.fillMaxWidth()) {
                Text("촬영 시작")
            }

            Spacer(Modifier.height(12.dp))

            Button(onClick = onOpenLog, modifier = Modifier.fillMaxWidth()) {
                Text("앱 내 로그")
            }

            Spacer(Modifier.height(12.dp))

            Button(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) {
                Text("설정")
            }
            Spacer(Modifier.height(12.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenTableEditor)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("표 상세설정", fontSize = 16.sp)
                    Spacer(Modifier.height(6.dp))
                    Text("Table Editor", fontSize = 12.sp, color = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("설정 화면(준비중)")
        Button(onClick = onBack) { Text("Back") }
    }
}

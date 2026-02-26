package com.dudoziworkshop.dzlog.ui.common

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Icon Language Layer (A안)
 * - 지금은 이모지로 렌더
 * - 나중에 drawable(네가 그린 이미지)로 교체할 때 이 파일만 바꾸면 됨
 */
sealed class DzIcon {
    @Composable
    abstract fun Render()

    class Directory(private val index: Int) : DzIcon() {
        @Composable
        override fun Render() {
            Text(
                text = "📁$index",
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }

    class NameTag(private val index: Int) : DzIcon() {
        @Composable
        override fun Render() {
            Text(
                text = "🏷$index",
                fontSize = 10.sp
            )
        }
    }
}

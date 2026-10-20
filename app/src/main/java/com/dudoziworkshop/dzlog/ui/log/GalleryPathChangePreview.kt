package com.dudoziworkshop.dzlog.ui.log

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.dudoziworkshop.dzlog.feature.log.policy.GalleryFolderIndexPolicy
import com.dudoziworkshop.dzlog.ui.theme.LocalDDZColor
import com.dudoziworkshop.dzlog.ui.theme.LocalDDZSpacing
import com.dudoziworkshop.dzlog.ui.theme.LocalDDZTypography

/** Presentation only: storage destinations remain unchanged. */
internal fun galleryReadablePath(path: String): String {
    val root = GalleryFolderIndexPolicy.ROOT.trimEnd('/')
    val normalized = path.trim().trimEnd('/')
    val suffix = when {
        normalized == root -> ""
        normalized.startsWith("$root/") -> normalized.removePrefix("$root/")
        else -> normalized
    }
    return if (suffix.isBlank()) "DZlog" else "DZlog / " + suffix.split('/')
        .filter { it.isNotBlank() }.joinToString(" / ")
}

@Composable
internal fun GalleryPathPreviewCard(label: String, pathText: String) {
    val colors = LocalDDZColor.current
    val spacing = LocalDDZSpacing.current
    val typography = LocalDDZTypography.current
    Surface(color = colors.SurfaceSoft, shape = MaterialTheme.shapes.medium) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(spacing.controlGap),
            verticalArrangement = Arrangement.spacedBy(spacing.xs),
        ) {
            Text(label, style = typography.Caption, color = colors.TextSecondary)
            Text(
                text = pathText,
                modifier = Modifier.fillMaxWidth(),
                style = typography.SettingLabel,
                color = colors.TextPrimary,
            )
        }
    }
}

@Composable
internal fun GalleryPathChangeArrow() {
    Text(
        text = "↓",
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
        style = LocalDDZTypography.current.SettingLabel,
        color = LocalDDZColor.current.TextSecondary,
    )
}

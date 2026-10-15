package com.dudoziworkshop.dzlog.ui.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography

/** Onboarding copy and visuals share the product design tokens, not mock camera controls. */
@Composable
internal fun OnboardingBrandIntro() {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Spacer(Modifier.height(8.dp))
        Text("DZlog · PHOTO RECORD", style = DDZTypography.Caption, color = DDZColor.SelectedDark)
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text("사진을,", style = DDZTypography.HomeMainTitle, color = DDZColor.TextPrimary)
            Text("더 가치 있는 기록으로.", style = DDZTypography.HomeMainTitle, color = DDZColor.PrimaryDark)
        }
        Text("일상부터 현장까지.", style = DDZTypography.SectionTitle, color = DDZColor.SelectedDark)
        Text(
            "사진에 필요한 정보를 함께 담고,\n나만의 방식으로 정리해 보세요.",
            style = DDZTypography.Body, color = DDZColor.TextSecondary,
        )
        OnboardingPhotoShowcase(
            labels = listOf("기록명", "날짜", "번호"),
            values = listOf("관찰 기록", "26.10.15", "001"),
            height = 230.dp,
        )
        Text(
            "사진과 기록표를 함께 보여주는 구성 예시예요.",
            style = DDZTypography.Caption,
            color = DDZColor.TextSecondary,
        )
    }
}

@Composable
private fun IntroConcept(label: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, color = DDZColor.Surface, shape = RoundedCornerShape(14.dp)) {
        Column(
            Modifier.padding(vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(icon, contentDescription = null, tint = DDZColor.PrimaryDark)
            Text(label, style = DDZTypography.SettingLabel, color = DDZColor.TextPrimary)
        }
    }
}

@Composable
internal fun OnboardingFeatureIntro() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "사진에 기록을 더하고,\n정리까지 한 번에.",
            style = DDZTypography.HomeMainTitle,
            color = DDZColor.TextPrimary,
        )
        Text(
            "촬영부터 보관까지, DZlog의 핵심 기능을 소개할게요.",
            style = DDZTypography.Body,
            color = DDZColor.TextSecondary,
        )
        OnboardingFeatureCard(
            icon = Icons.Default.CameraAlt,
            title = "사진과 기록, 내 방식대로.",
            detail = "원하는 기록표를 사진에 담거나, 기록표 없이 자유롭게 촬영해요.",
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FeatureModeLabel("기록표 촬영")
                FeatureModeLabel("표 없이 촬영")
            }
            OnboardingPhotoShowcase(
                labels = listOf("기록명", "날짜", "번호"),
                values = listOf("관찰 기록", "26.10.15", "001"),
                height = 150.dp,
                compact = true,
            )
        }
        OnboardingFeatureCard(
            icon = Icons.Default.Folder,
            title = "촬영과 동시에, 원하는 곳에.",
            detail = "직접 설정한 파일명과 폴더 규칙에 따라 사진을 저장해요.",
        )
        OnboardingFeatureCard(
            icon = Icons.Default.Description,
            title = "쌓인 기록도, 찾기 쉽게.",
            detail = "저장된 사진을 확인하고 폴더별로 관리해요.",
        )
    }
}

@Composable
private fun FeatureModeLabel(label: String) {
    Surface(color = DDZColor.SelectedSoft, shape = RoundedCornerShape(8.dp)) {
        Text(
            label,
            style = DDZTypography.Caption,
            color = DDZColor.SelectedDark,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun OnboardingFeatureCard(
    icon: ImageVector,
    title: String,
    detail: String,
    extra: @Composable () -> Unit = {},
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = DDZColor.Surface,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, DDZColor.Border),
    ) {
        Row(
            Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(icon, contentDescription = null, tint = DDZColor.PrimaryDark)
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text(title, style = DDZTypography.SettingLabel, color = DDZColor.TextPrimary)
                Text(detail, style = DDZTypography.Secondary, color = DDZColor.TextSecondary)
                extra()
            }
        }
    }
}

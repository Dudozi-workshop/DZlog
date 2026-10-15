package com.dudoziworkshop.dzlog.ui.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography

/** Editorial introduction. All examples are non-persistent. */
@Composable
internal fun OnboardingBrandIntro() {
    Column(verticalArrangement = Arrangement.spacedBy(13.dp)) {
        Spacer(Modifier.height(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("사진을,", style = DDZTypography.HomeMainTitle, color = DDZColor.TextPrimary)
            Text("더 가치 있는 기록으로.", style = DDZTypography.HomeMainTitle,
                color = DDZColor.PrimaryDark)
        }
        Text("일상부터 현장까지,", style = DDZTypography.SectionTitle,
            color = DDZColor.SelectedDark)
        Text("사진과 기록을 함께 남겨보세요.", style = DDZTypography.Body,
            color = DDZColor.TextSecondary)
        OnboardingPhotoShowcase(
            labels = listOf("기록명", "날짜", "번호"),
            values = listOf("관찰 기록", "26.10.15", "001"),
            height = 270.dp,
        )
        Text("한 장의 사진이, 더 오래 기억되는 기록이 됩니다.",
            style = DDZTypography.Caption, color = DDZColor.TextSecondary)
    }
}

@Composable
internal fun OnboardingFeatureIntro() {
    var showRecordTable by rememberSaveable { mutableStateOf(true) }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("사진에 기록을 더하고,\\n정리까지 한 번에.",
            style = DDZTypography.HomeMainTitle, color = DDZColor.TextPrimary)
        Text("촬영부터 보관까지, 필요한 기능만 담았어요.",
            style = DDZTypography.Body, color = DDZColor.TextSecondary)
        Surface(
            color = DDZColor.SelectedSoft.copy(alpha = 0.55f),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("01  사진과 기록, 내 방식대로.",
                    style = DDZTypography.SectionTitle, color = DDZColor.TextPrimary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FeatureModeLabel("기록표 촬영", showRecordTable) { showRecordTable = true }
                    FeatureModeLabel("표 없이 촬영", !showRecordTable) { showRecordTable = false }
                }
                OnboardingPhotoShowcase(
                    labels = listOf("기록명", "날짜", "번호"),
                    values = listOf("관찰 기록", "26.10.15", "001"),
                    height = 146.dp,
                    compact = true,
                    showTable = showRecordTable,
                )
            }
        }
        CompactFeature(
            icon = Icons.Default.Folder,
            number = "02",
            title = "촬영과 동시에, 원하는 곳에.",
            detail = "내 파일명과 폴더 규칙에 맞춰 저장해요.",
        )
        CompactFeature(
            icon = Icons.Default.Description,
            number = "03",
            title = "쌓인 기록도, 찾기 쉽게.",
            detail = "최근 사진과 폴더를 바로 찾아보세요.",
        )
    }
}

@Composable
private fun FeatureModeLabel(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        color = if (isSelected) DDZColor.SelectedSoft else DDZColor.Surface,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, if (isSelected) DDZColor.Selected else DDZColor.Border),
        modifier = Modifier.clickable(role = Role.RadioButton, onClick = onClick)
            .semantics { selected = isSelected },
    ) {
        Text(
            label, style = DDZTypography.Caption,
            color = if (isSelected) DDZColor.SelectedDark else DDZColor.TextSecondary,
            modifier = Modifier.heightIn(min = 40.dp)
                .padding(horizontal = 14.dp, vertical = 10.dp),
        )
    }
}

@Composable
private fun CompactFeature(
    icon: ImageVector,
    number: String,
    title: String,
    detail: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = DDZColor.PrimaryDark)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text("$"+"number  "+ "$"+"title", style = DDZTypography.SettingLabel,
                color = DDZColor.TextPrimary)
            Text(detail, style = DDZTypography.Caption, color = DDZColor.TextSecondary)
        }
    }
}

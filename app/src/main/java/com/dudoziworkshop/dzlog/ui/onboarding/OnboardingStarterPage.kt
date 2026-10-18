package com.dudoziworkshop.dzlog.ui.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dudoziworkshop.dzlog.data.template.OnboardingStarterCatalog
import com.dudoziworkshop.dzlog.data.template.OnboardingStarterDefinition
import com.dudoziworkshop.dzlog.domain.model.TableCellDataType
import com.dudoziworkshop.dzlog.domain.naming.buildFileName
import com.dudoziworkshop.dzlog.domain.naming.buildSavePath
import com.dudoziworkshop.dzlog.domain.table.TableResolver
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import java.util.Date

/** Read-only starter picker. All writes remain behind the existing onChoose callback. */
@Composable
internal fun OnboardingStarterPage(
    category: String,
    selected: OnboardingStarterDefinition,
    compact: Boolean,
    onCategory: (String) -> Unit,
    onBrowse: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(if (compact) 11.dp else 14.dp)) {
        Box(Modifier.fillMaxWidth()) {
            Column(
                verticalArrangement = Arrangement.spacedBy(5.dp),
                modifier = Modifier.padding(end = 2.dp),
            ) {
                Text(
                    "어떤 기록을 시작할까요?",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontSize = 23.sp, lineHeight = 29.sp, fontWeight = FontWeight.Bold,
                    ),
                    color = DDZColor.TextPrimary,
                )
                Text(
                    "나에게 맞는 양식을 골라보세요. 나중에도 바꿀 수 있어요.",
                    fontSize = 13.sp, lineHeight = 19.sp,
                    color = DDZColor.TextSecondary,
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("기록 유형", fontSize = 15.sp, fontWeight = FontWeight.Bold,
                    color = DDZColor.TextPrimary)
                Text("총 ${OnboardingStarterCatalog.definitions.size}개 양식",
                    fontSize = 12.sp, color = DDZColor.TextSecondary)
            }
            OnboardingStarterCategories(
                selectedId = category, onSelect = onCategory, compact = compact,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(selected.title, fontSize = 18.sp, fontWeight = FontWeight.Bold,
                    color = DDZColor.TextPrimary)
                if (category != "basic") {
                    Text(
                        "다른 양식 보기", color = DDZColor.PrimaryDark,
                        fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable(onClick = onBrowse)
                            .padding(horizontal = 3.dp, vertical = 8.dp),
                    )
                }
            }
            OnboardingStarterTablePreview(selected, compact)
        }
        StarterSaveRules(selected, compact)
        StarterChangeNotice()
    }
}

@Composable
private fun OnboardingStarterTablePreview(def: OnboardingStarterDefinition, compact: Boolean) {
    val state = remember(def.id) { requireNotNull(OnboardingStarterCatalog.buildState(def.id)) }
    val values = state.cells.filter { it.rowIndex == 1 }.map { cell ->
        when (cell.dataType) {
            TableCellDataType.DATE -> "자동 날짜"
            TableCellDataType.COUNTER -> "001"
            else -> "입력"
        }
    }
    Surface(
        color = DDZColor.Surface,
        shape = RoundedCornerShape(19.dp),
        border = BorderStroke(1.dp, DDZColor.Border.copy(alpha = 0.42f)),
        shadowElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
                .padding(horizontal = 13.dp, vertical = if (compact) 11.dp else 14.dp),
            verticalArrangement = Arrangement.spacedBy(if (compact) 9.dp else 12.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("선택한 양식 미리보기", fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold, color = DDZColor.PrimaryDark)
                Text(
                    when (def.category) {
                        "basic" -> "기본 촬영"
                        "daily" -> "일상·취미"
                        else -> "업무·현장"
                    },
                    modifier = Modifier.background(
                        DDZColor.SelectedSoft, RoundedCornerShape(50),
                    ).padding(horizontal = 10.dp, vertical = 5.dp),
                    fontSize = 11.sp, color = DDZColor.SelectedDark,
                    fontWeight = FontWeight.Medium,
                )
            }
            Column(
                modifier = Modifier.fillMaxWidth()
                    .background(DDZColor.Surface, RoundedCornerShape(13.dp)),
            ) {
                StarterTableRow(def.labels, Color(0xFFF1E6DA), strong = true, compact = compact)
                StarterTableRow(values, DDZColor.Surface, strong = false, compact = compact)
            }
            Text(
                "기록표 숨기기 가능 · 입력값은 빈칸에서 시작",
                color = DDZColor.TextSecondary, fontSize = 11.sp,
                lineHeight = 16.sp,
            )
        }
    }
}

@Composable
private fun StarterTableRow(cells: List<String>, color: Color, strong: Boolean, compact: Boolean) {
    Row(Modifier.fillMaxWidth()) {
        cells.forEach { value ->
            Box(
                modifier = Modifier.weight(1f)
                    .background(color)
                    .then(Modifier.padding(1.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    value,
                    modifier = Modifier.fillMaxWidth().padding(
                        horizontal = 2.dp, vertical = if (compact) 11.dp else 14.dp,
                    ),
                    fontSize = if (compact) 11.sp else 12.sp,
                    lineHeight = 16.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    fontWeight = if (strong) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (strong) DDZColor.PrimaryDark else DDZColor.TextPrimary,
                )
            }
        }
    }
}

@Composable
private fun StarterSaveRules(def: OnboardingStarterDefinition, compact: Boolean) {
    val state = remember(def.id) { requireNotNull(OnboardingStarterCatalog.buildState(def.id)) }
    val now = remember(def.id) { Date() }
    val resolved = remember(state, now) {
        TableResolver().plan(
            cells = state.cells, captureNow = now,
            config = TableResolver.Config(3, "yyyyMMdd", "HHmm"),
        ).resolvedCells
    }
    val fileName = remember(state, resolved) {
        buildFileName(resolved, state.fileNameSlotDrafts, "_", 3, 1, now,
            "yyyyMMdd", "HHmm")
    }
    val savePath = remember(state, resolved) {
        buildSavePath(resolved, state.pathSlotDrafts, now, "yyyyMMdd", "HHmm")
    }
    var expanded by rememberSaveable(def.id) { mutableStateOf(false) }
    Surface(
        color = DDZColor.Surface, shape = RoundedCornerShape(17.dp),
        border = BorderStroke(1.dp, DDZColor.Border.copy(alpha = 0.45f)),
    ) {
        Column(
            Modifier.fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = if (compact) 2.dp else 4.dp),
        ) {
            Row(
                Modifier.fillMaxWidth().clickable { expanded = !expanded }
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(if (expanded) "저장 규칙 접기" else "저장 규칙 확인",
                    fontWeight = FontWeight.SemiBold, fontSize = 15.sp,
                    color = DDZColor.PrimaryDark)
                Icon(
                    if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) "접기" else "펼치기",
                    tint = DDZColor.PrimaryDark,
                )
            }
            if (expanded) {
                Text("파일명", fontSize = 11.sp, color = DDZColor.TextSecondary)
                Text(fileName, style = MaterialTheme.typography.bodySmall,
                    color = DDZColor.TextPrimary, maxLines = 2,
                    overflow = TextOverflow.Ellipsis)
                Text("저장 위치", modifier = Modifier.padding(top = 8.dp),
                    fontSize = 11.sp, color = DDZColor.TextSecondary)
                Text(savePath, modifier = Modifier.padding(bottom = 12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = DDZColor.TextPrimary, maxLines = 2,
                    overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun StarterChangeNotice() {
    Surface(
        color = DDZColor.AmbientWarm,
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 13.dp, vertical = 11.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(34.dp).background(DDZColor.SurfaceSoft, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Description, contentDescription = null,
                    modifier = Modifier.size(19.dp), tint = DDZColor.PrimaryDark)
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("시작 후에도 양식과 저장 설정을 바꿀 수 있어요.",
                    fontSize = 12.sp, lineHeight = 17.sp,
                    fontWeight = FontWeight.SemiBold, color = DDZColor.TextPrimary)
                Text("기존 사진과 저장된 양식은 그대로 유지됩니다.",
                    fontSize = 11.sp, lineHeight = 16.sp,
                    color = DDZColor.TextSecondary)
            }
        }
    }
}

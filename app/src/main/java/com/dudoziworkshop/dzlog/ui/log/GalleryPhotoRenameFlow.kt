package com.dudoziworkshop.dzlog.ui.log

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.dudoziworkshop.dzlog.data.mediastore.GalleryPhotoRenameSnapshot
import com.dudoziworkshop.dzlog.domain.naming.NamingFormatDefaults.FILE_NAME_DELIMITER
import com.dudoziworkshop.dzlog.feature.log.policy.GalleryPhotoName
import com.dudoziworkshop.dzlog.feature.log.policy.GalleryPhotoRenamePlan
import com.dudoziworkshop.dzlog.feature.log.policy.GalleryPhotoRenamePolicy
import com.dudoziworkshop.dzlog.feature.log.policy.GalleryPhotoRenameResult
import com.dudoziworkshop.dzlog.feature.log.policy.GalleryPhotoRenameRow
import com.dudoziworkshop.dzlog.feature.log.policy.GalleryPhotoRenameRule
import com.dudoziworkshop.dzlog.feature.log.policy.GalleryRenameOutcome
import com.dudoziworkshop.dzlog.feature.log.policy.GalleryRenameResolution
import com.dudoziworkshop.dzlog.feature.log.policy.GalleryRenameStatus
import com.dudoziworkshop.dzlog.ui.common.DDZButtonStyle
import com.dudoziworkshop.dzlog.ui.common.DDZDialogAction
import com.dudoziworkshop.dzlog.ui.theme.LocalDDZColor
import com.dudoziworkshop.dzlog.ui.theme.LocalDDZSpacing
import com.dudoziworkshop.dzlog.ui.theme.LocalDDZTypography

private enum class RenamePage { Input, Phrases, Advanced, Preview, Resolve }
private enum class RenameMode { Exact, Position, Literal }
private enum class PreviewFilter(val label: String) {
    All("전체"), Ready("변경 가능"), Excluded("제외"), Unchanged("변경 없음");

    fun includes(status: GalleryRenameStatus) = when (this) {
        All -> true
        Ready -> status == GalleryRenameStatus.Ready
        Excluded -> status in setOf(GalleryRenameStatus.Conflict, GalleryRenameStatus.Check, GalleryRenameStatus.Excluded)
        Unchanged -> status == GalleryRenameStatus.Unchanged
    }
}

@Composable
internal fun GalleryPhotoRenameFlow(
    snapshot: GalleryPhotoRenameSnapshot,
    busy: Boolean,
    result: GalleryPhotoRenameResult?,
    executionError: String?,
    onDismiss: () -> Unit,
    onExecute: (GalleryPhotoRenamePlan) -> Unit,
) {
    var page by rememberSaveable { mutableStateOf(RenamePage.Input) }
    var mode by rememberSaveable { mutableStateOf(RenameMode.Exact) }
    var phrase by rememberSaveable { mutableStateOf("") }
    var position by rememberSaveable { mutableStateOf(-1) }
    var literal by rememberSaveable { mutableStateOf("") }
    var replacement by rememberSaveable { mutableStateOf("") }
    var advancedPhrases by rememberSaveable { mutableStateOf(arrayListOf<String>()) }
    var advancedReplacement by rememberSaveable { mutableStateOf("") }
    var advancedPreview by rememberSaveable { mutableStateOf(false) }
    var filter by rememberSaveable { mutableStateOf(PreviewFilter.All) }
    var resolutionIds by rememberSaveable { mutableStateOf(longArrayOf()) }
    var savedChoices by rememberSaveable { mutableStateOf(arrayListOf<String>()) }
    var draftChoices by rememberSaveable { mutableStateOf(arrayListOf<String>()) }
    var manualInputs by rememberSaveable { mutableStateOf(arrayListOf<String>()) }
    var search by rememberSaveable { mutableStateOf("") }

    val examples = remember(snapshot) { snapshot.groups.map { GalleryPhotoName.parse(it.oldName) } }
    val representative = examples.indexOfFirst { it.components != null }.coerceAtLeast(0)
    val sample = examples[representative]
    val phrases = remember(examples) {
        examples.flatMap { it.components.orEmpty().distinct() }.groupingBy { it }.eachCount()
            .entries.sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
    }
    val basicRule = when (mode) {
        RenameMode.Exact -> GalleryPhotoRenameRule.Exact(phrase, replacement).takeIf { phrase.isNotEmpty() }
        RenameMode.Position -> GalleryPhotoRenameRule.Position(position, replacement).takeIf { position >= 0 }
        RenameMode.Literal -> GalleryPhotoRenameRule.Literal(literal, replacement).takeIf { literal.isNotEmpty() }
    }
    val mergeRule = GalleryPhotoRenameRule.Merge(advancedPhrases.toSet(), advancedReplacement)
    val rule = when (page) {
        RenamePage.Advanced -> mergeRule
        RenamePage.Preview, RenamePage.Resolve -> if (advancedPreview) mergeRule else basicRule
        RenamePage.Input, RenamePage.Phrases -> basicRule
    }
    val choices = remember(savedChoices) { decodeRenameChoices(savedChoices) }
    val draft = remember(draftChoices) { decodeRenameChoices(draftChoices) }
    val manualById = remember(manualInputs) { decodeRenameChoices(manualInputs) }

    fun planFor(rule: GalleryPhotoRenameRule?, resolutions: Map<Long, GalleryRenameResolution>) =
        rule?.let { runCatching {
            GalleryPhotoRenamePolicy.plan(snapshot.groups, snapshot.photos, it, resolutions, snapshot.otherFiles)
        } }
    val planned = remember(snapshot, rule, choices) { planFor(rule, choices) }
    val plan = planned?.getOrNull()
    val draftPlan = if (page == RenamePage.Resolve) remember(snapshot, rule, draft) {
        planFor(rule, draft)?.getOrNull()
    } else null
    val validation = planned?.exceptionOrNull()?.message
    val canPreview = rule != null && validation == null

    fun goBack() {
        if (busy) return
        if (result != null) {
            onDismiss()
            return
        }
        page = when (page) {
            RenamePage.Input -> { onDismiss(); RenamePage.Input }
            RenamePage.Phrases, RenamePage.Advanced -> RenamePage.Input
            RenamePage.Preview -> if (advancedPreview) RenamePage.Advanced else RenamePage.Input
            RenamePage.Resolve -> RenamePage.Preview
        }
    }

    fun openPreview(advanced: Boolean) {
        advancedPreview = advanced
        savedChoices = arrayListOf()
        manualInputs = arrayListOf()
        filter = PreviewFilter.All
        page = RenamePage.Preview
    }

    val title = if (result != null) "이름 변경 결과" else when (page) {
        RenamePage.Input -> "사진 이름 일괄 변경"
        RenamePage.Phrases -> "전체 문구"
        RenamePage.Advanced -> "여러 문구 통합"
        RenamePage.Preview -> "이름 변경 미리보기"
        RenamePage.Resolve -> "중복 이름 해결"
    }
    val input = page == RenamePage.Input && result == null
    PhotoRenamePanel(
        title = title,
        compact = input,
        onBack = ::goBack,
        footer = {
            when {
                result != null -> RenameActions(primary = "확인", onPrimary = onDismiss)
                page == RenamePage.Input -> RenameActions("취소", "미리보기", ::goBack, canPreview) {
                    openPreview(advanced = false)
                }
                page == RenamePage.Advanced -> RenameActions("뒤로", "미리보기", ::goBack, canPreview) {
                    openPreview(advanced = true)
                }
                page == RenamePage.Phrases -> RenameActions(primary = "뒤로", onPrimary = ::goBack)
                page == RenamePage.Preview -> RenameActions(
                    "뒤로", if (busy) "처리 중" else "변경", ::goBack,
                    enabled = !busy && plan?.ready?.isNotEmpty() == true, backEnabled = !busy,
                ) { plan?.let(onExecute) }
                else -> RenameActions("취소", "적용", ::goBack, enabled = draftPlan?.rows?.none {
                    it.group.id in resolutionIds && it.status == GalleryRenameStatus.Conflict &&
                        it.resolution != null
                } == true) {
                    savedChoices = draftChoices
                    page = RenamePage.Preview
                }
            }
        },
    ) {
        val spacing = LocalDDZSpacing.current
        when {
            result != null -> RenameReport(result)
            page == RenamePage.Input -> Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(spacing.controlGap),
            ) {
                RenameChoices {
                    RenameChip(mode != RenameMode.Literal, "구성 선택") {
                        if (mode == RenameMode.Literal) mode = RenameMode.Exact
                    }
                    RenameChip(mode == RenameMode.Literal, "직접 입력") { mode = RenameMode.Literal }
                }
                if (mode == RenameMode.Literal) {
                    RenameField(literal, { literal = it }, "찾을 문구")
                } else {
                    RenameChoices {
                        RenameChip(mode == RenameMode.Exact, "같은 문구") { mode = RenameMode.Exact }
                        RenameChip(mode == RenameMode.Position, "같은 위치") { mode = RenameMode.Position }
                    }
                    Text(snapshot.groups[representative].oldName, style = LocalDDZTypography.current.Body)
                    RenameChoices {
                        sample.components.orEmpty().forEachIndexed { index, part ->
                            val label = if (mode == RenameMode.Position) "${index + 1}. $part" else part
                            RenameChip(if (mode == RenameMode.Position) position == index else phrase == part, label) {
                                if (mode == RenameMode.Position) position = index else phrase = part
                            }
                        }
                        if (mode == RenameMode.Position) {
                            val maxParts = examples.maxOf { it.components?.size ?: 0 }
                            for (index in sample.components.orEmpty().size until maxParts) {
                                RenameChip(position == index, "${index + 1}번째 자리") { position = index }
                            }
                        }
                    }
                    if (mode == RenameMode.Exact && phrases.isNotEmpty()) {
                        RenameChoices {
                            phrases.take(4).forEach { (text, count) ->
                                RenameChip(phrase == text, "$text · $count") { phrase = text }
                            }
                            if (phrases.size > 4) TextButton(onClick = { search = ""; page = RenamePage.Phrases }) {
                                Text("전체 문구")
                            }
                        }
                    }
                }
                RenameField(replacement, { replacement = it },
                    if (mode == RenameMode.Literal) "바꿀 문구 (비우면 삭제)" else "새 문구")
                if (plan != null) RenameSummary(plan)
                else RenameHint("선택한 사진 ${snapshot.groups.size}장")
                if (replacement.isNotEmpty()) validation?.let { RenameHint(it, error = true) }
                TextButton(onClick = { search = ""; page = RenamePage.Advanced }) { Text("고급 설정") }
            }
            page == RenamePage.Phrases || page == RenamePage.Advanced -> {
                val advanced = page == RenamePage.Advanced
                LazyColumn(verticalArrangement = Arrangement.spacedBy(spacing.controlGap)) {
                    item {
                        RenameField(search, { search = it }, "문구 검색")
                        if (advanced) {
                            RenameField(advancedReplacement, { advancedReplacement = it }, "통합할 새 문구")
                            RenameHint("${advancedPhrases.size}개 선택 · 두 개 이상 선택")
                            if (advancedReplacement.isNotEmpty()) validation?.let { RenameHint(it, error = true) }
                        }
                    }
                    items(phrases.filter { (text) -> text.contains(search.trim(), ignoreCase = true) }, key = { it.key }) {
                        (text, count) ->
                        RenameChip(if (advanced) text in advancedPhrases else phrase == text, "$text · $count") {
                            if (advanced) {
                                advancedPhrases = ArrayList(if (text in advancedPhrases) advancedPhrases - text
                                    else advancedPhrases + text)
                            } else {
                                phrase = text
                                page = RenamePage.Input
                            }
                        }
                    }
                }
            }
            page == RenamePage.Preview && plan != null -> LazyColumn(
                verticalArrangement = Arrangement.spacedBy(spacing.controlGap),
            ) {
                item {
                    RenameHint(rule?.description().orEmpty())
                    RenameSummary(plan)
                    executionError?.let { RenameHint(it, error = true) }
                    RenameChoices {
                        PreviewFilter.entries.forEach { value ->
                            RenameChip(filter == value, value.label, enabled = !busy) { filter = value }
                        }
                    }
                }
                items(plan.rows.filter { filter.includes(it.status) }, key = { it.group.id }) { row ->
                    RenamePreviewRow(row, rule as? GalleryPhotoRenameRule.Position)
                    if (row.status == GalleryRenameStatus.Conflict || row.resolution != null) {
                        TextButton(enabled = !busy, onClick = {
                            val members = plan.conflictGroup(row.group.id)
                            resolutionIds = members.filter { id ->
                                plan.rows.any { it.group.id == id && (it.proposedName != it.group.oldName || it.resolution != null) }
                            }.toLongArray()
                            draftChoices = savedChoices
                            page = RenamePage.Resolve
                        }) { Text("해결") }
                    }
                    HorizontalDivider()
                }
            }
            page == RenamePage.Resolve && draftPlan != null -> LazyColumn(
                verticalArrangement = Arrangement.spacedBy(spacing.controlGap),
            ) {
                val members = draftPlan.rows.filter { it.group.id in resolutionIds }
                if (members.size > 1) item {
                    TextButton(onClick = {
                        val automatic = members.filter { it.status == GalleryRenameStatus.Conflict &&
                            draft[it.group.id] !is GalleryRenameResolution.Manual }
                            .associate { it.group.id to GalleryRenameResolution.Automatic }
                        draftChoices = encodeRenameChoices(draft + automatic)
                    }) { Text("그룹 자동 해결") }
                }
                items(members, key = { it.group.id }) { row ->
                    val id = row.group.id
                    val choice = draft[id]
                    val original = GalleryPhotoName.parse(row.group.oldName)
                    fun choose(value: GalleryRenameResolution) {
                        draftChoices = encodeRenameChoices(draft + (id to value))
                        if (value is GalleryRenameResolution.Manual) {
                            manualInputs = encodeRenameChoices(manualById + (id to value))
                        }
                    }

                    RenamePreviewRow(row, rule as? GalleryPhotoRenameRule.Position)
                    RenameChoices {
                        RenameChip(choice == GalleryRenameResolution.Automatic, "자동 해결") {
                            choose(GalleryRenameResolution.Automatic)
                        }
                        RenameChip(choice is GalleryRenameResolution.Manual, "직접 수정") {
                            choose(manualById[id]
                                ?: GalleryRenameResolution.Manual(GalleryPhotoName.parse(row.finalName).prefix))
                        }
                        val excluded = choice == GalleryRenameResolution.Exclude ||
                            (choice == null && row.status == GalleryRenameStatus.Conflict)
                        RenameChip(excluded, "그대로 제외") { choose(GalleryRenameResolution.Exclude) }
                    }
                    if (choice is GalleryRenameResolution.Manual) {
                        RenameField(choice.prefix, { choose(GalleryRenameResolution.Manual(it)) }, "최종 이름")
                        if (original.protectedSuffix.isNotEmpty()) RenameHint("고정되는 뒷부분 ${original.protectedSuffix}")
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun PhotoRenamePanel(
    title: String,
    compact: Boolean,
    onBack: () -> Unit,
    footer: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    val spacing = LocalDDZSpacing.current
    val colors = LocalDDZColor.current
    val panel: @Composable () -> Unit = {
        Surface(
            modifier = if (compact) Modifier.fillMaxWidth() else Modifier.fillMaxSize(),
            shape = if (compact) MaterialTheme.shapes.large else MaterialTheme.shapes.extraSmall,
            color = colors.Surface,
            contentColor = colors.TextPrimary,
        ) {
            Layout(
                modifier = Modifier.padding(horizontal = spacing.sectionGap, vertical = spacing.controlGap),
                content = {
                    Text(
                        title, Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
                        style = LocalDDZTypography.current.SectionTitle,
                    )
                    Box(Modifier.fillMaxWidth()) { content() }
                    Box(Modifier.fillMaxWidth()) { footer() }
                },
            ) { children, constraints ->
                val loose = constraints.copy(minHeight = 0)
                val header = children[0].measure(loose)
                val actions = children[2].measure(loose)
                val gap = spacing.controlGap.roundToPx()
                val afterActions = (constraints.maxHeight - actions.height - gap).coerceAtLeast(0)
                // Keep one control reachable when the keyboard leaves very little room.
                val showHeader = afterActions >= header.height + gap + 48.dp.roundToPx()
                val headerSpace = if (showHeader) header.height + gap else 0
                val bodyHeight = (afterActions - headerSpace).coerceAtLeast(0)
                val body = children[1].measure(loose.copy(
                    minHeight = if (compact) 0 else bodyHeight,
                    maxHeight = bodyHeight,
                ))
                val height = (headerSpace + body.height + gap + actions.height)
                    .coerceIn(constraints.minHeight, constraints.maxHeight)
                layout(constraints.maxWidth, height) {
                    if (showHeader) header.placeRelative(0, 0)
                    body.placeRelative(0, headerSpace)
                    actions.placeRelative(0, height - actions.height)
                }
            }
        }
    }
    if (compact) {
        Dialog(
            onDismissRequest = onBack,
            properties = DialogProperties(decorFitsSystemWindows = false),
        ) {
            Box(Modifier.fillMaxWidth().safeDrawingPadding().imePadding(), contentAlignment = Alignment.Center) {
                panel()
            }
        }
    } else {
        BackHandler(onBack = onBack)
        // Keep full-screen pages in the activity window so system bars constrain the whole layout.
        Surface(
            modifier = Modifier.fillMaxSize().semantics { paneTitle = title },
            color = colors.Surface,
        ) {
            Box(Modifier.fillMaxSize().safeDrawingPadding().imePadding()) { panel() }
        }
    }
}

@Composable
private fun RenameActions(
    secondary: String? = null,
    primary: String,
    onSecondary: () -> Unit = {},
    enabled: Boolean = true,
    backEnabled: Boolean = true,
    onPrimary: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(LocalDDZSpacing.current.controlGap),
    ) {
        if (secondary != null) DDZDialogAction(
            onClick = onSecondary, modifier = Modifier.weight(1f).fillMaxHeight().sizeIn(minHeight = 48.dp),
            enabled = backEnabled, style = DDZButtonStyle.Secondary,
        ) { Text(secondary, textAlign = TextAlign.Center) }
        DDZDialogAction(
            onClick = onPrimary, modifier = Modifier.weight(1f).fillMaxHeight().sizeIn(minHeight = 48.dp),
            enabled = enabled,
        ) { Text(primary, textAlign = TextAlign.Center) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RenameChoices(content: @Composable () -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(LocalDDZSpacing.current.controlGap)) { content() }
}

@Composable
private fun RenameChip(selected: Boolean, label: String, enabled: Boolean = true, onClick: () -> Unit) {
    val colors = LocalDDZColor.current
    FilterChip(selected = selected, onClick = onClick, enabled = enabled, label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = colors.SelectedSoft, selectedLabelColor = colors.SelectionEmphasis,
        ))
}

@Composable
private fun RenameField(value: String, onChange: (String) -> Unit, label: String) {
    OutlinedTextField(value, onChange, Modifier.fillMaxWidth(), label = { Text(label) }, singleLine = true)
}

@Composable
private fun RenameHint(text: String, error: Boolean = false) {
    val colors = LocalDDZColor.current
    Text(text, style = LocalDDZTypography.current.Caption, color = if (error) colors.Destructive else colors.TextSecondary)
}

@Composable
private fun RenameSummary(plan: GalleryPhotoRenamePlan) {
    val count = PreviewFilter.entries.drop(1).joinToString(" · ") { filter ->
        "${filter.label} ${plan.rows.count { filter.includes(it.status) }}"
    }
    Text(count, style = LocalDDZTypography.current.Body)
}

@Composable
private fun RenamePreviewRow(row: GalleryPhotoRenameRow, position: GalleryPhotoRenameRule.Position?) {
    val before = GalleryPhotoName.parse(row.group.oldName)
    val parts = before.components
    val emphasis = LocalDDZColor.current.SelectionEmphasis
    Text(buildAnnotatedString {
        append("기존  ")
        if (position == null || parts == null || position.index !in parts.indices) append(row.group.oldName)
        else {
            parts.forEachIndexed { index, part ->
                if (index > 0) append(FILE_NAME_DELIMITER)
                if (index == position.index) withStyle(SpanStyle(color = emphasis, fontWeight = FontWeight.Bold)) {
                    append(part)
                } else append(part)
            }
            append(before.protectedSuffix)
        }
    })
    if (row.status != GalleryRenameStatus.Unchanged && row.finalName != row.group.oldName) {
        Text("변경 후  ${row.finalName}")
    }
    if (row.group.photos.size > 1) RenameHint("원본 포함 ${row.group.photos.size}파일")
    when (row.resolution) {
        GalleryRenameResolution.Automatic -> RenameHint("자동 해결")
        is GalleryRenameResolution.Manual -> RenameHint("직접 지정")
        GalleryRenameResolution.Exclude -> RenameHint("제외")
        null -> Unit
    }
    row.issue?.let { RenameHint(it, error = true) }
}

@Composable
private fun RenameReport(result: GalleryPhotoRenameResult) {
    val labels = mapOf(
        GalleryRenameOutcome.Success to "성공", GalleryRenameOutcome.Failure to "실패",
        GalleryRenameOutcome.Excluded to "제외", GalleryRenameOutcome.Unchanged to "유지",
        GalleryRenameOutcome.RecoveryNeeded to "복구 필요",
    )
    val spacing = LocalDDZSpacing.current
    LazyColumn(verticalArrangement = Arrangement.spacedBy(spacing.controlGap)) {
        item {
            Text(labels.entries.joinToString(" · ") { (outcome, label) ->
                "$label ${result.rows.count { it.outcome == outcome }}"
            })
            RenameHint("변경 성공 파일 ${result.renamedIds.size}개")
        }
        items(result.rows, key = { it.preview.group.id }) { row ->
            Text("${labels.getValue(row.outcome)}  ${row.preview.group.oldName}")
            if (row.outcome == GalleryRenameOutcome.Success) Text(row.preview.finalName)
            row.issue?.let { RenameHint(it, error = row.outcome != GalleryRenameOutcome.Success) }
            row.preview.group.photos.forEach { file ->
                row.recoveryNames[file.id]?.let { current ->
                    RenameHint("${file.relativePath}$current → ${file.displayName}", error = true)
                }
            }
            HorizontalDivider()
        }
    }
}

private fun GalleryPhotoRenameRule.description() = when (this) {
    is GalleryPhotoRenameRule.Exact -> "$phrase → $replacement"
    is GalleryPhotoRenameRule.Position -> "${index + 1}번째 자리 → $replacement"
    is GalleryPhotoRenameRule.Literal -> "$find → ${replacement.ifEmpty { "(삭제)" }}"
    is GalleryPhotoRenameRule.Merge -> "${phrases.joinToString(", ")} → $replacement"
}

private fun encodeRenameChoices(choices: Map<Long, GalleryRenameResolution>): ArrayList<String> =
    ArrayList(choices.map { (id, value) ->
        when (value) {
            GalleryRenameResolution.Automatic -> "$id:A"
            GalleryRenameResolution.Exclude -> "$id:E"
            is GalleryRenameResolution.Manual -> "$id:M:${value.prefix}"
        }
    })

private fun decodeRenameChoices(choices: List<String>): Map<Long, GalleryRenameResolution> =
    choices.associate { entry ->
        val parts = entry.split(':', limit = 3)
        parts[0].toLong() to when (parts[1]) {
            "A" -> GalleryRenameResolution.Automatic
            "E" -> GalleryRenameResolution.Exclude
            else -> GalleryRenameResolution.Manual(parts[2])
        }
    }

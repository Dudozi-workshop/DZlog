package com.dudoziworkshop.dzlog.ui.table.rotating

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.dudoziworkshop.dzlog.domain.model.RotatingPhraseSet
import com.dudoziworkshop.dzlog.domain.phrase.RotatingPhraseSetItemPolicy
import com.dudoziworkshop.dzlog.ui.common.DDZAlertDialog
import com.dudoziworkshop.dzlog.ui.common.DDZButtonStyle
import com.dudoziworkshop.dzlog.ui.common.DDZDialogAction
import com.dudoziworkshop.dzlog.ui.common.DDZTopBar
import com.dudoziworkshop.dzlog.ui.common.DDZTopBarIconButton
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography

/**
 * Keep the set period above the phrase list and the numbering/CSV actions below it.
 * Scaffold's bottom bar reserves the footer before the phrase list gets the remaining height.
 */
@Composable
fun RotatingPhraseSetEditDialog(
    phraseSet: RotatingPhraseSet,
    onClose: () -> Unit,
    onUpdateSet: ((RotatingPhraseSet) -> RotatingPhraseSet) -> Unit,
    onDeleteSet: (String) -> Unit,
) {
    var showMenu by remember(phraseSet.id) { mutableStateOf(false) }
    var showRename by remember(phraseSet.id) { mutableStateOf(false) }
    var showDelete by remember(phraseSet.id) { mutableStateOf(false) }
    var showItem by remember(phraseSet.id) { mutableStateOf(false) }
    var editingIndex by remember(phraseSet.id) { mutableStateOf<Int?>(null) }
    var editingText by remember(phraseSet.id) { mutableStateOf("") }
    var continuousAdd by remember(phraseSet.id) { mutableStateOf(true) }
    var periodTargets by remember(phraseSet.id) { mutableStateOf<List<Int>>(emptyList()) }

    fun editItem(index: Int?, text: String) {
        editingIndex = index
        editingText = text
        showItem = true
    }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize().imePadding(),
            containerColor = DDZColor.Background,
            // Full-screen dialogs can report a consumed/zero nav inset on some devices.
            // The bottomBar takes its own measurable height, including a conservative
            // fallback for system navigation, so CSV can never be covered by the gesture bar.
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                val navigationHeight = with(LocalDensity.current) {
                    WindowInsets.navigationBars.getBottom(this).toDp()
                }
                val missingNavigationSpace = (48.dp - navigationHeight).coerceAtLeast(0.dp)
                Column(
                    modifier = Modifier.fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(start = 12.dp, end = 12.dp, bottom = 16.dp + missingNavigationSpace),
                ) {
                    RotatingPhraseSetFooter(
                        phraseSet = phraseSet,
                        onUpdateSet = onUpdateSet,
                    )
                }
            },
            topBar = {
                DDZTopBar(
                    title = phraseSet.name,
                    onBack = onClose,
                    actions = {
                        androidx.compose.foundation.layout.Box {
                            DDZTopBarIconButton(Icons.Default.MoreVert, "세트 메뉴", { showMenu = true })
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false },
                            ) {
                                DropdownMenuItem(
                                    text = { Text("세트 이름 변경") },
                                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                    onClick = {
                                        showMenu = false
                                        showRename = true
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text("세트 삭제", color = DDZColor.Destructive) },
                                    leadingIcon = {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = DDZColor.Destructive)
                                    },
                                    onClick = {
                                        showMenu = false
                                        showDelete = true
                                    },
                                )
                            }
                        }
                    },
                )
            },
        ) { insets ->
            BoxWithConstraints(
                modifier = Modifier.fillMaxSize().padding(insets)
                    .padding(start = 12.dp, end = 12.dp, top = 6.dp),
            ) {
                // The pinned bottomBar has already been subtracted by Scaffold.
                // Only the list consumes the remaining measured viewport height.
                // Very short windows may scroll their content without moving the footer.
                val shortScreen = maxHeight < 360.dp
                val compactListHeight = maxHeight * 0.45f
                Column(
                    modifier = if (shortScreen) {
                        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                    } else {
                        Modifier.fillMaxSize()
                    },
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    RotatingPhraseDefaultPeriodBar(
                        phraseSet = phraseSet,
                        onUpdateSet = onUpdateSet,
                    )
                    RotatingPhraseSetListPanel(
                        phraseSet = phraseSet,
                        modifier = if (shortScreen) {
                            Modifier.fillMaxWidth().height(compactListHeight)
                        } else {
                            Modifier.weight(1f).fillMaxWidth()
                        },
                        onUpdateSet = onUpdateSet,
                        onEditItem = { index, text -> editItem(index, text) },
                        onEditPeriod = { index -> periodTargets = listOf(index) },
                        onBulkPeriod = { indices -> periodTargets = indices },
                        onAddItem = { editItem(null, "") },
                    )
                }
            }
        }
    }

    if (showRename) {
        var nameDraft by remember(showRename, phraseSet.id) { mutableStateOf(phraseSet.name) }
        DDZAlertDialog(
            onDismissRequest = { showRename = false },
            title = { Text("세트 이름 변경") },
            text = {
                OutlinedTextField(
                    value = nameDraft,
                    onValueChange = { nameDraft = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("세트 이름") },
                )
            },
            confirmButton = {
                DDZDialogAction(
                    style = DDZButtonStyle.Primary,
                    enabled = nameDraft.isNotBlank(),
                    onClick = {
                        val name = nameDraft.trim()
                        if (name.isNotBlank()) onUpdateSet { it.copy(name = name) }
                        showRename = false
                    },
                ) { Text("적용", style = DDZTypography.ButtonText) }
            },
            dismissButton = {
                DDZDialogAction(
                    style = DDZButtonStyle.Secondary,
                    onClick = { showRename = false },
                ) { Text("취소", style = DDZTypography.ButtonText) }
            },
        )
    }

    if (showItem) {
        val index = editingIndex
        RotatingPhraseItemEditDialog(
            initialText = editingText,
            isEdit = index != null,
            continuousAdd = continuousAdd,
            canAdd = phraseSet.items.size < RotatingPhraseSet.MAX_ITEMS,
            onContinuousAddChange = { continuousAdd = it },
            onDismiss = { showItem = false },
            onSave = { text ->
                if (index == null) {
                    onUpdateSet { RotatingPhraseSetItemPolicy.addItem(it, text) }
                } else {
                    onUpdateSet { RotatingPhraseSetItemPolicy.renameItem(it, index, text) }
                }
            },
        )
    }

    if (periodTargets.isNotEmpty()) {
        val targets = periodTargets.filter { it in phraseSet.items.indices }
        if (targets.isNotEmpty()) {
            val intervals = targets.map { phraseSet.itemEveryOverrides.getOrNull(it) }
            val existing = if (intervals.distinct().size == 1) intervals.first() else null
            RotatingPhrasePeriodDialog(
                initialEvery = existing,
                defaultEvery = phraseSet.defaultEvery,
                targetLabel = phraseSet.items[targets.first()],
                selectedCount = targets.size,
                mixedValues = intervals.distinct().size > 1,
                onDismiss = { periodTargets = emptyList() },
                onApply = { every ->
                    onUpdateSet { set ->
                        RotatingPhraseSetItemPolicy.setEveryForItems(set, targets, every)
                    }
                    periodTargets = emptyList()
                },
            )
        }
    }

    if (showDelete) {
        DDZAlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text("세트 삭제") },
            text = { Text("${phraseSet.name} 세트를 삭제하시겠습니까?") },
            confirmButton = {
                DDZDialogAction(
                    style = DDZButtonStyle.Destructive,
                    onClick = { onDeleteSet(phraseSet.id) },
                ) { Text("삭제", style = DDZTypography.ButtonText) }
            },
            dismissButton = {
                DDZDialogAction(
                    style = DDZButtonStyle.Secondary,
                    onClick = { showDelete = false },
                ) { Text("취소", style = DDZTypography.ButtonText) }
            },
        )
    }
}

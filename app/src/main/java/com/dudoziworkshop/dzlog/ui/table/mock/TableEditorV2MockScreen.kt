package com.dudoziworkshop.dzlog.ui.table.mock

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler as AndroidBackHandler
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.dudoziworkshop.dzlog.data.template.SavedTableTemplate
import com.dudoziworkshop.dzlog.domain.model.SaveMode
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.domain.preview.PreviewState
import com.dudoziworkshop.dzlog.feature.table.model.TableStyleState

/** Android persistence/counter/back behavior stays in the original application boundary. */
@Composable
fun TableEditorV2Screen(
    templateState: TableTemplateState,
    includePathInCounterScope: Boolean,
    includeFilenameInCounterScope: Boolean,
    saveMode: SaveMode,
    counterPadding: Int,
    styleState: TableStyleState,
    isUnsavedNewTemplate: Boolean = false,
    openSaveSettingsInitially: Boolean = false,
    onSave: suspend (
        TableTemplateState,
        TableStyleState,
        Boolean,
        Boolean,
        SaveMode,
        Int,
        Int?,
        Boolean,
    ) -> Boolean,
    onDiscardUnsavedNewTemplate: () -> Unit = {},
    onBack: () -> Unit,
    templateId: String? = null,
    templateName: String = "새 템플릿",
    templates: List<SavedTableTemplate> = emptyList(),
    hasPendingTemplateSelection: Boolean = false,
    onSwitchTemplate: (String) -> Unit = {},
) {
    val context = LocalContext.current
    val platform = remember(context) { AndroidTableEditorPlatform(context) }
    TableEditorV2Content(
        platform = platform, templateState = templateState,
        includePathInCounterScope = includePathInCounterScope,
        includeFilenameInCounterScope = includeFilenameInCounterScope,
        saveMode = saveMode, counterPadding = counterPadding, styleState = styleState,
        isUnsavedNewTemplate = isUnsavedNewTemplate,
        openSaveSettingsInitially = openSaveSettingsInitially,
        onSave = onSave, onDiscardUnsavedNewTemplate = onDiscardUnsavedNewTemplate,
        onBack = onBack, templateId = templateId, templateName = templateName,
        templates = templates, hasPendingTemplateSelection = hasPendingTemplateSelection,
        onSwitchTemplate = onSwitchTemplate,
    )
}

private class AndroidTableEditorPlatform(private val context: Context) : TableEditorPlatform {
    @Composable override fun Preview(session: TableEditorV2SessionState): PreviewState =
        tableEditorPreview(context, session)
    @Composable override fun Counter(session: TableEditorV2SessionState, active: Boolean): TableEditorCounterActions =
        rememberTableEditorCounterOrchestrator(context, session, active)
    @Composable override fun HandleBack(onBack: () -> Unit) { AndroidBackHandler(onBack = onBack) }
    override fun notify(message: String) { Toast.makeText(context, message, Toast.LENGTH_SHORT).show() }
}

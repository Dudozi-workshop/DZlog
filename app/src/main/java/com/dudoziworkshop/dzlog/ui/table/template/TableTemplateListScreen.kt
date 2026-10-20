package com.dudoziworkshop.dzlog.ui.table.template

import androidx.compose.runtime.Composable
import com.dudoziworkshop.dzlog.data.template.SavedTableTemplate
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Android keeps its locale/date adapter and all existing caller-owned mutations. */
@Composable
fun TableTemplateListScreen(
    templates: List<SavedTableTemplate>,
    activeTemplateId: String?,
    onBack: () -> Unit,
    onOpenTemplate: (String) -> Unit,
    onSelectTemplate: (String) -> Unit,
    onCreateTemplate: () -> Unit,
    onRenameTemplate: (String, String) -> Unit,
    onDuplicateTemplate: (String) -> Unit,
    onDeleteTemplate: (String) -> Unit,
) {
    TableTemplateListContent(
        templates = templates, activeTemplateId = activeTemplateId, onBack = onBack,
        onOpenTemplate = onOpenTemplate, onSelectTemplate = onSelectTemplate,
        onCreateTemplate = onCreateTemplate, onRenameTemplate = onRenameTemplate,
        onDuplicateTemplate = onDuplicateTemplate, onDeleteTemplate = onDeleteTemplate,
        formatDate = ::formatTemplateDate,
    )
}

private fun formatTemplateDate(timestamp: Long, used: Boolean): String {
    if (timestamp <= 0L) return if (used) "사용 기록 없음" else "수정일 없음"
    return (if (used) "사용 " else "수정 ") + SimpleDateFormat("yyyy.MM.dd", Locale.getDefault()).format(Date(timestamp))
}

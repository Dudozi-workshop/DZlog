package com.dudoziworkshop.dzlog.domain.project

/**
 * Projects remember only the ID of a shared template. Switching capture
 * contexts must never clone template values, counters, or cyclic phrase state.
 */
internal data class ProjectTemplateChoice(
    val templateId: String?,
    val rememberedTemplateMissing: Boolean,
)

internal object ProjectTemplateSelectionPolicy {
    fun choose(
        registry: CaptureProjectRegistry,
        projectId: String?,
        availableTemplateIds: List<String>,
        currentTemplateId: String?,
    ): ProjectTemplateChoice {
        require(projectId == null || registry.find(projectId)?.isActive == true) {
            "Only an open project can become a capture target"
        }
        val remembered = if (projectId == null) registry.generalLastTemplateId
        else registry.find(projectId)?.lastTemplateId
        val validIds = availableTemplateIds.filter(String::isNotBlank).distinct()
        val chosen = remembered?.takeIf(validIds::contains)
            ?: currentTemplateId?.takeIf(validIds::contains)
            ?: validIds.firstOrNull()
        return ProjectTemplateChoice(
            templateId = chosen,
            rememberedTemplateMissing = remembered != null && remembered !in validIds,
        )
    }
}

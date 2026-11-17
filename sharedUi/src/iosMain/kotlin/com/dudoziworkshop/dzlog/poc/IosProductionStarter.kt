package com.dudoziworkshop.dzlog.poc

import com.dudoziworkshop.dzlog.data.template.OnboardingStarterCatalog
import com.dudoziworkshop.dzlog.data.template.SavedTableTemplate
import com.dudoziworkshop.dzlog.feature.table.model.TableStyleState

/** Shared catalog data with an iOS-stable id; existing persisted workspaces always win. */
internal fun iosProductionStarterTemplate(
    starterId: String = OnboardingStarterCatalog.DEFAULT_ID,
): SavedTableTemplate {
    val starter = requireNotNull(OnboardingStarterCatalog.find(starterId))
    return SavedTableTemplate(
        id = "ios-starter-${starter.id}",
        name = starter.title,
        modifiedAt = 0,
        templateState = requireNotNull(OnboardingStarterCatalog.buildState(starter.id)),
        styleState = TableStyleState(),
    )
}

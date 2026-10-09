package com.dudoziworkshop.dzlog.feature.table.policy

import android.content.Context
import androidx.datastore.preferences.core.edit
import com.dudoziworkshop.dzlog.data.preferences.KEY_TABLE_TEMPLATE_JSON
import com.dudoziworkshop.dzlog.data.preferences.dataStore
import com.dudoziworkshop.dzlog.data.template.toJsonString
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState

suspend fun saveTableTemplate(context: Context, templateState: TableTemplateState): Result<Unit> = runCatching {
    context.dataStore.edit { prefs ->
        prefs[KEY_TABLE_TEMPLATE_JSON] = templateState.toJsonString()
    }
}
